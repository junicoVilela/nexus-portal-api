package br.com.softon.portal;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import br.com.softon.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteRepository;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseModuloVersaoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
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
 * Exercita a jornada de entrega sem depender de dados de desenvolvimento:
 * PostgreSQL, migrações e massa de contrato/release existem apenas durante o teste.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ReleaseOrchestratorEntregaIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Value("${local.server.port}") int port;
  @Autowired OrchestratorClienteRepository clienteRepository;
  @Autowired ProdutoRhRepository produtoRepository;
  @Autowired ModuloProdutoRepository moduloRepository;
  @Autowired OrchestratorClienteProdutoRepository clienteProdutoRepository;
  @Autowired OrchestratorClienteProdutoModuloRepository clienteProdutoModuloRepository;
  @Autowired ReleaseRepository releaseRepository;
  @Autowired ReleaseModuloVersaoRepository releaseModuloVersaoRepository;

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void criaEntregaInicializaModulosEGeraDownloadsReais() throws Exception {
    MassaEntrega massa = criarMassaIsolada();
    String token = login();

    HttpResponse<String> criacao = enviarJson("/api/v1/release-orchestrator/entregas", token,
        Map.of(
            "clienteId", massa.cliente().getId(),
            "produtoId", massa.produto().getId(),
            "releaseId", massa.release().getId(),
            "ambiente", AmbientePadrao.HOM.name(),
            "observacoes", "Entrega gerada por Testcontainers"));
    assertThat(criacao.statusCode()).isEqualTo(201);
    UUID entregaId = UUID.fromString(json.readTree(criacao.body()).path("id").asText());

    HttpResponse<String> modulos = postSemCorpo(
        "/api/v1/release-orchestrator/entregas/" + entregaId + "/modulos/inicializar", token);
    assertThat(modulos.statusCode()).isEqualTo(200);
    JsonNode selecao = json.readTree(modulos.body());
    assertThat(selecao).hasSize(1);
    assertThat(selecao.get(0).path("selecionado").asBoolean()).isTrue();
    assertThat(selecao.get(0).path("versaoTo").asText()).isEqualTo("2.0.0");

    HttpResponse<String> inicio = postSemCorpo(
        "/api/v1/release-orchestrator/entregas/" + entregaId + "/geracao/iniciar", token);
    assertThat(inicio.statusCode()).isEqualTo(202);

    JsonNode entrega = aguardarConclusao(entregaId, token);
    assertThat(entrega.path("status").asText()).isEqualTo("CONCLUIDA");
    assertThat(entrega.path("arquivoPacoteCaminho").asText()).isNotBlank();

    HttpResponse<byte[]> zip = baixar(
        "/api/v1/release-orchestrator/entregas/" + entregaId + "/pacote/download", token);
    assertThat(zip.statusCode()).isEqualTo(200);
    assertThat(zip.headers().firstValue("content-disposition").orElse("")).contains(".zip");
    assertThat(zip.body()).startsWith((byte) 0x50, (byte) 0x4b);

    HttpResponse<byte[]> pdf = baixar(
        "/api/v1/release-orchestrator/entregas/" + entregaId + "/documento", token);
    assertThat(pdf.statusCode()).isEqualTo(200);
    assertThat(new String(pdf.body(), 0, 4)).isEqualTo("%PDF");
  }

  private MassaEntrega criarMassaIsolada() {
    String sufixo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    ProdutoRh produto = produtoRepository.save(new ProdutoRh(
        "Produto E2E " + sufixo, "E2E" + sufixo, "Produto isolado de teste", "#2563EB", null, true));
    Cliente cliente = clienteRepository.save(
        new Cliente("Cliente E2E " + sufixo, "CLI" + sufixo, AmbientePadrao.HOM));
    ClienteProduto contrato = clienteProdutoRepository.save(
        new ClienteProduto(cliente, produto, AmbientePadrao.HOM));
    ModuloProduto modulo = moduloRepository.save(new ModuloProduto(produto, "FUNC", "Funcionalidades",
        TipoModulo.FUNCIONALIDADES, false, true, 1, null));
    clienteProdutoModuloRepository.save(new ClienteProdutoModulo(contrato, modulo, "1.0.0"));

    Release release = releaseRepository.save(new Release(produto, "2.0.0", "Release E2E " + sufixo,
        TipoRelease.FEATURE, ReleaseStatus.RASCUNHO, LocalDate.now(), null,
        "Release criada somente no banco temporário", null));
    release.publicar("teste-e2e");
    releaseRepository.save(release);
    releaseModuloVersaoRepository.save(new ReleaseModuloVersao(release, modulo, "2.0.0"));
    return new MassaEntrega(cliente, produto, release);
  }

  private JsonNode aguardarConclusao(UUID entregaId, String token) throws Exception {
    for (int tentativa = 0; tentativa < 40; tentativa++) {
      HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(
          URI.create(url("/api/v1/release-orchestrator/entregas/" + entregaId)))
          .header("Authorization", "Bearer " + token).GET().build(),
          HttpResponse.BodyHandlers.ofString());
      assertThat(resposta.statusCode()).isEqualTo(200);
      JsonNode entrega = json.readTree(resposta.body());
      String status = entrega.path("status").asText();
      if ("CONCLUIDA".equals(status)) return entrega;
      if ("FALHA".equals(status)) {
        throw new AssertionError("A geração falhou: " + entrega.path("erroGeracao").asText());
      }
      Thread.sleep(250);
    }
    throw new AssertionError("A geração da entrega não terminou dentro do prazo do teste.");
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

  private HttpResponse<String> postSemCorpo(String path, String token) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create(url(path)))
        .header("Authorization", "Bearer " + token)
        .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<byte[]> baixar(String path, String token) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create(url(path)))
        .header("Authorization", "Bearer " + token).GET().build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private record MassaEntrega(Cliente cliente, ProdutoRh produto, Release release) {}
}
