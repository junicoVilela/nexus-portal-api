package br.com.softon.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sobe Postgres efêmero, aplica V1–V17 e valida que os 3 usuários seed conseguem
 * logar e que o /auth/me devolve o grupo RBAC esperado. Pega regressões nos seeds
 * (ex.: bug do hash BCrypt do `revisor` em V12) antes do smoke manual.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthSeedsIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Value("${local.server.port}")
  int port;

  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void admin_loga_e_pertence_ao_grupo_ADMIN() throws Exception {
    Map<String, Object> me = loginEBuscarMe("admin", "admin");
    assertThat(grupos(me)).containsExactly("ADMIN");
    assertThat(permissoes(me)).hasSizeGreaterThan(50);
  }

  @Test
  void editor_loga_e_pertence_ao_grupo_EDITOR() throws Exception {
    Map<String, Object> me = loginEBuscarMe("editor", "editor");
    assertThat(grupos(me)).containsExactly("EDITOR");
    assertThat(permissoes(me)).contains("CLIENTE:CRIAR", "PAGINA:EDITAR", "PUBLICACAO:LER");
  }

  @Test
  void revisor_loga_e_pertence_ao_grupo_REVISOR() throws Exception {
    Map<String, Object> me = loginEBuscarMe("revisor", "revisor");
    assertThat(grupos(me)).containsExactly("REVISOR");
    assertThat(permissoes(me))
        .containsExactlyInAnyOrder(
            "CLIENTE:LER",
            "MODULO:LER",
            "PAGINA:EDITAR",
            "PAGINA:LER",
            "PROJETO:LER",
            "PUBLICACAO:LER");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loginEBuscarMe(String username, String password) throws Exception {
    String loginBody = json.writeValueAsString(Map.of("username", username, "password", password));
    HttpResponse<String> loginResp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/auth/login")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(loginResp.statusCode()).isEqualTo(200);
    Map<String, Object> loginJson = json.readValue(loginResp.body(), Map.class);
    String token = (String) loginJson.get("token");
    assertThat(token).isNotBlank();

    HttpResponse<String> meResp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/auth/me")))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(meResp.statusCode()).isEqualTo(200);
    return json.readValue(meResp.body(), Map.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  @SuppressWarnings("unchecked")
  private List<String> grupos(Map<String, Object> me) {
    List<Map<String, Object>> raw = (List<Map<String, Object>>) me.get("grupos");
    return raw.stream().map(g -> (String) g.get("codigo")).toList();
  }

  @SuppressWarnings("unchecked")
  private List<String> permissoes(Map<String, Object> me) {
    return (List<String>) me.get("permissoes");
  }
}
