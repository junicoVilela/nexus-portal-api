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
import java.util.List;
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

/** Onda D de ponta a ponta: chave de integração → manual vigente, tela, site, help-bridge, CORS, MCP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "nexus.ai.enabled=true")
@ActiveProfiles("test")
@Testcontainers
class ManualIntegracaoIntegrationTest {

  private static final String ORIGEM = "https://app.acme.com";

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
  void sistemaDoClienteUsaOManualVigentePelaChave() throws Exception {
    String sufixo = UUID.randomUUID().toString().substring(0, 8);
    String codigo = "PED-" + sufixo;
    Cliente cliente = clienteRepository.save(new Cliente("ACME " + sufixo, "acme-" + sufixo, true));
    Projeto projeto = projetoRepository.save(new Projeto("Vendas " + sufixo, "vendas-" + sufixo, null, true));
    Modulo modulo = moduloRepository.save(new Modulo("Pedidos", "pedidos-" + sufixo, null, 1, true, projeto));
    clienteModuloRepository.save(new ClienteModulo(cliente, modulo));
    Pagina pagina = new Pagina("Consulta de pedidos", "consulta-" + sufixo, codigo, "Localizar pedidos",
        "<h2>Filtros</h2><p>Filtre os pedidos por período e status.</p>", 1, true, modulo, null);
    pagina.publicar();
    paginaRepository.save(pagina);

    String jwt = login();
    UUID publicacaoId = UUID.fromString(json.readTree(post("/api/v1/docflow/publicacoes", jwt,
        Map.of("clienteId", cliente.getId(), "versao", "2.0.0")).body()).path("id").asText());
    aguardar(publicacaoId, jwt);

    var criada = post("/api/v1/docflow/clientes/" + cliente.getId() + "/acessos-manual", jwt,
        Map.of("nome", "NEXUS-LD", "origens", List.of(ORIGEM)));
    assertThat(criada.statusCode()).isEqualTo(201);
    JsonNode chave = json.readTree(criada.body());
    String token = chave.path("token").asText();
    assertThat(token).startsWith("nxm_");

    JsonNode vigente = json.readTree(get("/api/v1/manual/" + token + "/vigente", null).body());
    assertThat(vigente.path("versao").asText()).isEqualTo("2.0.0");

    JsonNode tela = json.readTree(get("/api/v1/manual/" + token + "/tela/" + codigo.toLowerCase(), null).body());
    assertThat(tela.path("url").asText()).isEqualTo("site/paginas/consulta-" + sufixo + ".html");
    var ausente = get("/api/v1/manual/" + token + "/tela/NF-999", null);
    assertThat(ausente.statusCode()).as("404 de negócio, não 500").isEqualTo(404);
    assertThat(json.readTree(ausente.body()).path("message").asText()).contains("NF-999");

    var site = get("/api/v1/manual/" + token + "/site/" + tela.path("url").asText().substring(5), null);
    assertThat(site.statusCode()).isEqualTo(200);
    assertThat(site.headers().firstValue("content-type").orElse("")).startsWith("text/html");
    assertThat(site.body()).contains("Consulta de pedidos");
    assertThat(get("/api/v1/manual/" + token + "/site/assets/routes.js", null).body()).contains(codigo);
    // %2F é barrado pelo firewall do Spring Security (400); ".." cru, pelo controller (404).
    assertThat(get("/api/v1/manual/" + token + "/site/..%2F..%2Fetc%2Fpasswd", null).statusCode()).isIn(400, 404);

    var bridge = get("/api/v1/manual/help-bridge.js", null);
    assertThat(bridge.headers().firstValue("content-type").orElse("")).contains("javascript");
    assertThat(bridge.body()).contains("window.NexusManual");

    var preflight = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/ai/manual/" + token + "/perguntar")))
        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
        .header("Origin", ORIGEM)
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "content-type")
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(preflight.headers().firstValue("access-control-allow-origin")).hasValue(ORIGEM);

    var pergunta = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/ai/manual/" + token + "/perguntar")))
        .header("Content-Type", "application/json").header("Origin", ORIGEM)
        .POST(HttpRequest.BodyPublishers.ofString("{\"pergunta\":\"como filtrar pedidos por status\"}"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(pergunta.statusCode()).isEqualTo(200);
    assertThat(json.readTree(pergunta.body()).path("citacoes").get(0).path("codigoTela").asText()).isEqualTo(codigo);

    var mcp = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/mcp")))
        .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"listarTelas\",\"arguments\":{}}}"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(json.readTree(mcp.body()).path("result").path("content").get(0).path("text").asText()).contains(codigo);

    var revogada = http.send(HttpRequest.newBuilder(
            URI.create(url("/api/v1/docflow/acessos-manual/" + chave.path("acesso").path("id").asText())))
        .header("Authorization", "Bearer " + jwt).DELETE().build(), HttpResponse.BodyHandlers.ofString());
    assertThat(revogada.statusCode()).isEqualTo(204);
    assertThat(get("/api/v1/manual/" + token + "/vigente", null).statusCode()).isEqualTo(404);
  }

  private void aguardar(UUID id, String jwt) throws Exception {
    for (int i = 0; i < 40; i++) {
      String status = json.readTree(get("/api/v1/docflow/publicacoes/" + id, jwt).body()).path("status").asText();
      if (!status.equals("GERANDO")) {
        assertThat(status).isEqualTo("SUCESSO");
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

  private HttpResponse<String> get(String path, String jwt) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path))).GET();
    if (jwt != null) {
      request.header("Authorization", "Bearer " + jwt);
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String jwt, Object body) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path)))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    if (jwt != null) {
      request.header("Authorization", "Bearer " + jwt);
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
