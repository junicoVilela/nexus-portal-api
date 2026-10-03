package com.nexus.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.integration.github.AiGithubAssinatura;
import com.nexus.portal.ai.integration.github.AiGithubClient;
import com.nexus.portal.ai.integration.github.AiGithubClient.ArquivoPr;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.repository.PaginaRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase C de ponta a ponta (AI-612): webhook assinado → fila → proposta (LLM fake) → aceitar →
 * rascunho no DocFlow. O GitHub é simulado; o projeto/módulo vêm do seed do manual (V12).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "nexus.ai.enabled=true",
        "nexus.ai.github.webhook-secret=" + AiFilaPrIntegrationTest.SECRET,
        "nexus.ai.github.repositorios[0].nome=org/app",
        "nexus.ai.github.repositorios[0].projeto-id=a0000000-0000-4000-8000-000000000101",
        "nexus.ai.github.repositorios[0].modulo-id=a0000000-0000-4000-8000-000000000112"})
@ActiveProfiles("test")
@Testcontainers
class AiFilaPrIntegrationTest {

  static final String SECRET = "segredo-e2e";

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

  @Value("${local.server.port}") int port;
  @Autowired PaginaRepository paginaRepository;
  @MockitoBean AiGithubClient github;

  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void webhookSemAssinaturaValidaEhRecusadoSemLogin() throws Exception {
    byte[] corpo = payload(1, "Qualquer");
    assertThat(webhook(corpo, "sha256=00", "d-invalida").statusCode()).isEqualTo(403);
  }

  @Test
  void prDeTelaNovaViraPropostaQueEhAceitaComoRascunho() throws Exception {
    when(github.arquivos(eq("org/app"), eq(10), anyInt())).thenReturn(List.of(
        new ArquivoPr("src/app/pages/exportacao/exportacao.component.html", "added", 40, 0,
            "+<h1>Exportação de pedidos</h1>\n+<button>Exportar CSV</button>")));
    byte[] corpo = payload(10, "Tela de exportação de pedidos EXP-901");

    assertThat(webhook(corpo, AiGithubAssinatura.assinar(SECRET, corpo), "d-10").statusCode()).isEqualTo(202);
    assertThat(webhook(corpo, AiGithubAssinatura.assinar(SECRET, corpo), "d-10").statusCode())
        .as("reentrega").isEqualTo(204);

    String token = login();
    JsonNode item = aguardar(token, 10, i -> "PENDENTE".equals(i.path("proposta").path("status").asText()));
    assertThat(item.path("classificacao").asText()).isEqualTo("UI_NOVA");
    assertThat(item.path("pendente").asBoolean()).isTrue();

    HttpResponse<String> aceite = post("/api/v1/ai/fila-pr/" + item.path("id").asText() + "/aceitar", token, Map.of());
    assertThat(aceite.statusCode()).isEqualTo(200);
    UUID paginaId = UUID.fromString(json.readTree(aceite.body()).path("paginaId").asText());
    assertThat(paginaRepository.findById(paginaId)).get()
        .satisfies(p -> assertThat(p.getStatus()).isEqualTo(StatusPagina.RASCUNHO));

    JsonNode depois = buscar(token, item.path("id").asText());
    assertThat(depois.path("pendente").asBoolean()).isFalse();
    assertThat(depois.path("responsavel").asText()).isEqualTo("admin");
  }

  @Test
  void prDeTelaJaPublicadaAguardaVoltarParaRascunho() throws Exception {
    when(github.arquivos(eq("org/app"), eq(11), anyInt())).thenReturn(List.of(
        new ArquivoPr("src/app/pages/inicio/inicio.component.html", "modified", 2, 1, "-a\n+b")));
    byte[] corpo = payload(11, "Ajusta a tela DF-START");

    assertThat(webhook(corpo, AiGithubAssinatura.assinar(SECRET, corpo), "d-11").statusCode()).isEqualTo(202);

    JsonNode item = aguardar(login(), 11, i -> !"RECEBIDO".equals(i.path("status").asText()));
    assertThat(item.path("status").asText()).isEqualTo("AGUARDANDO_RASCUNHO");
    assertThat(item.path("codigoTela").asText()).isEqualTo("DF-START");
    assertThat(item.path("paginaId").asText()).isEqualTo("a0000000-0000-4000-8000-000000000201");
  }

  private static byte[] payload(int numero, String titulo) {
    return ("{\"action\":\"closed\",\"repository\":{\"full_name\":\"org/app\"},\"pull_request\":{"
        + "\"number\":" + numero + ",\"title\":\"" + titulo + "\",\"body\":\"Entrega da tela.\",\"merged\":true,"
        + "\"html_url\":\"https://github.com/org/app/pull/" + numero + "\",\"user\":{\"login\":\"dev\"},"
        + "\"base\":{\"ref\":\"main\"},\"merge_commit_sha\":\"abc\",\"merged_at\":\"2026-10-01T12:00:00Z\","
        + "\"labels\":[]}}").getBytes(StandardCharsets.UTF_8);
  }

  private HttpResponse<String> webhook(byte[] corpo, String assinatura, String delivery) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/ai/webhooks/github")))
        .header("Content-Type", "application/json")
        .header("X-GitHub-Event", "pull_request")
        .header("X-GitHub-Delivery", delivery)
        .header("X-Hub-Signature-256", assinatura)
        .POST(HttpRequest.BodyPublishers.ofByteArray(corpo)).build(), HttpResponse.BodyHandlers.ofString());
  }

  /** Espera o item do PR atingir o estado (processamento e geração são assíncronos). */
  private JsonNode aguardar(String token, int numero, Predicate<JsonNode> pronto) throws Exception {
    for (int tentativa = 0; tentativa < 80; tentativa++) {
      HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/ai/fila-pr?pendentes=false")))
          .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
      assertThat(resposta.statusCode()).isEqualTo(200);
      for (JsonNode item : json.readTree(resposta.body())) {
        if (item.path("numeroPr").asInt() == numero && pronto.test(item)) {
          return item;
        }
      }
      Thread.sleep(250);
    }
    throw new AssertionError("O PR #" + numero + " não chegou ao estado esperado na fila.");
  }

  private JsonNode buscar(String token, String id) throws Exception {
    return json.readTree(http.send(HttpRequest.newBuilder(URI.create(url("/api/v1/ai/fila-pr/" + id)))
        .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
  }

  private String login() throws Exception {
    HttpResponse<String> resposta = post("/api/v1/auth/login", null, Map.of("username", "admin", "password", "admin"));
    assertThat(resposta.statusCode()).isEqualTo(200);
    return json.readTree(resposta.body()).path("token").asText();
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
