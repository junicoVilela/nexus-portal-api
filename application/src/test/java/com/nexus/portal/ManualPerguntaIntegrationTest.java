package com.nexus.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.ClienteModulo;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.repository.ClienteModuloRepository;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ModuloRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.repository.ProjetoRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Onda E de ponta a ponta: publicação real → perguntar pelo portal e pelo token de prévia → MCP.
 * O corpus é o ZIP publicado: texto que só existe em rascunho nunca aparece (INT-505).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "nexus.ai.enabled=true")
@ActiveProfiles("test")
@Testcontainers
class ManualPerguntaIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

  @Value("${local.server.port}") int port;
  @Autowired ClienteRepository clienteRepository;
  @Autowired ProjetoRepository projetoRepository;
  @Autowired ModuloRepository moduloRepository;
  @Autowired PaginaRepository paginaRepository;
  @Autowired ClienteModuloRepository clienteModuloRepository;

  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void perguntaAoManualPublicadoPeloPortalPeloLeitorEPeloMcp() throws Exception {
    String sufixo = UUID.randomUUID().toString().substring(0, 8);
    Cliente cliente = clienteRepository.save(new Cliente("Cliente Q " + sufixo, "cliente-q-" + sufixo, true));
    Projeto projeto = projetoRepository.save(new Projeto("Vendas " + sufixo, "vendas-" + sufixo, null, true));
    Modulo modulo = moduloRepository.save(new Modulo("Pedidos", "pedidos-" + sufixo, null, 1, true, projeto));
    clienteModuloRepository.save(new ClienteModulo(cliente, modulo));
    Pagina publicada = new Pagina("Consulta de pedidos", "consulta-" + sufixo, "PED-" + sufixo, "Localizar pedidos",
        "<h2>Filtros</h2><p>Filtre os pedidos por período, status e vendedor.</p>", 1, true, modulo, null);
    publicada.publicar();
    paginaRepository.save(publicada);
    paginaRepository.save(new Pagina("Devoluções", "devolucoes-" + sufixo, "DEV-" + sufixo, "Rascunho",
        "<h2>Estorno</h2><p>Para estornar a devolução, use o botão Reembolsar.</p>", 2, true, modulo, null));

    String token = login();
    var criacao = post("/api/v1/docflow/publicacoes", token,
        Map.of("clienteId", cliente.getId(), "versao", "1.5.0", "observacao", "Onda E"));
    UUID publicacaoId = UUID.fromString(json.readTree(criacao.body()).path("id").asText());
    aguardarConclusao(publicacaoId, token);

    JsonNode resposta = json.readTree(post("/api/v1/ai/publicacoes/" + publicacaoId + "/perguntar", token,
        Map.of("pergunta", "Como filtrar pedidos por vendedor?")).body());
    assertThat(resposta.path("modo").asText()).isEqualTo("IA");
    assertThat(resposta.path("citacoes").get(0).path("codigoTela").asText()).isEqualTo("PED-" + sufixo);

    JsonNode rascunho = json.readTree(post("/api/v1/ai/publicacoes/" + publicacaoId + "/perguntar", token,
        Map.of("pergunta", "Como estornar e reembolsar uma devolução?")).body());
    assertThat(rascunho.path("modo").asText()).as("conteúdo em rascunho não entra").isEqualTo("NAO_SEI");

    String previewToken = json.readTree(post("/api/v1/preview-tokens?clienteId=" + cliente.getId(), token, Map.of())
        .body()).path("token").asText();
    var leitor = post("/api/v1/ai/manual/" + previewToken + "/perguntar", null,
        Map.of("pergunta", "filtrar pedidos por status"));
    assertThat(leitor.statusCode()).isEqualTo(200);
    assertThat(json.readTree(leitor.body()).path("manual").asText()).contains("v1.5.0");

    var mcp = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/mcp")))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + previewToken)
        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"paginaPorCodigo\",\"arguments\":{\"codigoTela\":\"PED-" + sufixo + "\"}}}"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(mcp.statusCode()).isEqualTo(200);
    assertThat(json.readTree(mcp.body()).path("result").path("content").get(0).path("text").asText())
        .contains("## Filtros", "vendedor");

    var semToken = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/mcp")))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(semToken.statusCode()).isEqualTo(401);
  }

  private void aguardarConclusao(UUID id, String token) throws Exception {
    for (int tentativa = 0; tentativa < 40; tentativa++) {
      var resposta = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/publicacoes/" + id)))
          .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
      if (!json.readTree(resposta.body()).path("status").asText().equals("GERANDO")) {
        assertThat(json.readTree(resposta.body()).path("status").asText()).isEqualTo("SUCESSO");
        return;
      }
      Thread.sleep(250);
    }
    throw new AssertionError("Publicação não terminou.");
  }

  private String login() throws Exception {
    return json.readTree(post("/api/v1/auth/login", null, Map.of("username", "admin", "password", "admin")).body())
        .path("token").asText();
  }

  private HttpResponse<String> post(String path, String token, Object body) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path)))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
