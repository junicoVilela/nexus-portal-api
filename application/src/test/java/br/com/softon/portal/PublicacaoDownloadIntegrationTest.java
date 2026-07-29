package br.com.softon.portal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.entity.ClienteModulo;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.repository.ClienteModuloRepository;
import br.com.softon.portal.docflow.repository.ClienteRepository;
import br.com.softon.portal.docflow.repository.ModuloRepository;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.docflow.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PublicacaoDownloadIntegrationTest {

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
  void geraPublicacaoRealEBaixaZipEPdf() throws Exception {
    String sufixo = UUID.randomUUID().toString().substring(0, 8);
    Cliente cliente = clienteRepository.save(new Cliente("Cliente E2E " + sufixo, "cliente-e2e-" + sufixo, true));
    Projeto projeto = projetoRepository.save(
        new Projeto("Projeto E2E " + sufixo, "projeto-e2e-" + sufixo, "Fluxo real", true));
    Modulo modulo = moduloRepository.save(
        new Modulo("Cadastros", "cadastros-" + sufixo, "Módulo E2E", 1, true, projeto));
    clienteModuloRepository.save(new ClienteModulo(cliente, modulo));
    Pagina pagina = new Pagina("Cadastrar fornecedor", "fornecedor-" + sufixo,
        "E2E_" + sufixo, "Cadastro validado no teste de integração",
        "<section class=\"objective-card\"><h2>Objetivo</h2><p>Cadastrar um fornecedor.</p></section>",
        1, true, modulo, null);
    pagina.publicar();
    paginaRepository.save(pagina);

    String token = login();
    HttpResponse<String> criacao = enviarJson("/api/v1/docflow/publicacoes", token,
        Map.of("clienteId", cliente.getId(), "versao", "1.0.0-e2e", "observacao", "Teste integrado"));
    assertThat(criacao.statusCode()).isEqualTo(201);
    UUID publicacaoId = UUID.fromString(json.readTree(criacao.body()).path("id").asText());

    JsonNode publicacao = aguardarConclusao(publicacaoId, token);
    assertThat(publicacao.path("status").asText()).isEqualTo("SUCESSO");
    assertThat(publicacao.path("quantidadePaginas").asInt()).isEqualTo(1);

    HttpResponse<byte[]> zip = baixar("/api/v1/docflow/publicacoes/" + publicacaoId + "/download", token);
    assertThat(zip.statusCode()).isEqualTo(200);
    assertThat(zip.headers().firstValue("content-disposition").orElse("")).contains(".zip");
    assertThat(zip.body()).startsWith((byte) 0x50, (byte) 0x4b);

    HttpResponse<byte[]> pdf = baixar("/api/v1/docflow/publicacoes/" + publicacaoId + "/download-pdf", token);
    assertThat(pdf.statusCode()).isEqualTo(200);
    assertThat(new String(pdf.body(), 0, 4)).isEqualTo("%PDF");
  }

  private JsonNode aguardarConclusao(UUID id, String token) throws Exception {
    for (int tentativa = 0; tentativa < 40; tentativa++) {
      HttpResponse<String> resposta = http.send(
          HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/publicacoes/" + id)))
              .header("Authorization", "Bearer " + token).GET().build(),
          HttpResponse.BodyHandlers.ofString());
      assertThat(resposta.statusCode()).isEqualTo(200);
      JsonNode body = json.readTree(resposta.body());
      if (!body.path("status").asText().equals("GERANDO")) return body;
      Thread.sleep(250);
    }
    throw new AssertionError("A publicação não terminou dentro do prazo do teste.");
  }

  private String login() throws Exception {
    HttpResponse<String> resposta = enviarJson("/api/v1/auth/login", null,
        Map.of("username", "admin", "password", "admin"));
    assertThat(resposta.statusCode()).isEqualTo(200);
    return json.readTree(resposta.body()).path("token").asText();
  }

  private HttpResponse<String> enviarJson(String path, String token, Object body) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path)))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    if (token != null) request.header("Authorization", "Bearer " + token);
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<byte[]> baixar(String path, String token) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create(url(path)))
        .header("Authorization", "Bearer " + token).GET().build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
