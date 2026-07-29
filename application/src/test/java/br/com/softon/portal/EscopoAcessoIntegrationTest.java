package br.com.softon.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cenário fim-a-fim do escopo por clienteId: admin cria dois clientes e
 * um usuário restrito a um deles; o usuário só enxerga o cliente permitido
 * e recebe 404 no outro. Depois valida a flag somente_leitura.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EscopoAcessoIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

  @Value("${local.server.port}")
  int port;

  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void escopo_restrito_a_um_cliente_bloqueia_o_outro() throws Exception {
    String adminToken = login("admin", "admin");

    // Relaxa política de senha para permitir a senha simples do usuário de teste
    relaxarPoliticaSenha(adminToken);

    // Cria dois clientes
    UUID clienteA = criarCliente(adminToken, "ACME Corp", "acme-corp");
    UUID clienteB = criarCliente(adminToken, "Contoso", "contoso");

    // Cria usuário de teste e vincula ao grupo LEITOR (para poder listar clientes)
    UUID leitorGrupoId = idDoGrupo(adminToken, "LEITOR");
    UUID usuarioTeste = criarUsuario(adminToken, "escopo-tester", "senha-forte-123");
    vincularUsuarioAoGrupo(adminToken, usuarioTeste, leitorGrupoId);

    // Cria escopo restringindo o usuário ao clienteA (read-write)
    criarEscopoUsuarioReadWrite(adminToken, usuarioTeste, clienteA);

    // Loga como o usuário de teste
    String testerToken = login("escopo-tester", "senha-forte-123");

    // Listagem devolve só clienteA
    List<Map<String, Object>> lista = listarClientes(testerToken);
    assertThat(lista).hasSize(1);
    assertThat(lista.get(0).get("id")).isEqualTo(clienteA.toString());

    // GET direto no clienteA funciona (200)
    int statusA = statusGet(testerToken, "/api/v1/docflow/clientes/" + clienteA);
    assertThat(statusA).isEqualTo(200);

    // GET direto no clienteB devolve 404 (não vaza existência)
    int statusB = statusGet(testerToken, "/api/v1/docflow/clientes/" + clienteB);
    assertThat(statusB).isEqualTo(404);
  }

  @Test
  void escopo_somente_leitura_bloqueia_write() throws Exception {
    String adminToken = login("admin", "admin");
    relaxarPoliticaSenha(adminToken);

    UUID cliente = criarCliente(adminToken, "ReadOnly Client", "readonly-client");

    // Editor tem CLIENTE:EDITAR, então damos esse grupo pro tester
    UUID editorGrupoId = idDoGrupo(adminToken, "EDITOR");
    UUID usuarioTeste = criarUsuario(adminToken, "ro-tester", "senha-forte-123");
    vincularUsuarioAoGrupo(adminToken, usuarioTeste, editorGrupoId);

    // Escopo read-only cobrindo o único cliente que ele vê
    criarEscopoUsuarioReadOnly(adminToken, usuarioTeste, cliente);

    String testerToken = login("ro-tester", "senha-forte-123");

    // GET funciona
    assertThat(statusGet(testerToken, "/api/v1/docflow/clientes/" + cliente)).isEqualTo(200);

    // PUT falha com 400 (BusinessException → error handler devolve 4xx)
    String body = json.writeValueAsString(Map.of("nome", "Novo Nome", "ativo", true));
    HttpResponse<String> put = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/clientes/" + cliente)))
            .header("Authorization", "Bearer " + testerToken)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(put.statusCode()).isBetween(400, 499);
    assertThat(put.body()).contains("somente leitura");
  }

  // ---------- helpers HTTP ----------

  private String login(String username, String password) throws Exception {
    String body = json.writeValueAsString(Map.of("username", username, "password", password));
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/auth/login")))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    return (String) json.readValue(resp.body(), Map.class).get("token");
  }

  private void relaxarPoliticaSenha(String token) throws Exception {
    String body = json.writeValueAsString(Map.of(
        "tamanhoMinimo", 8,
        "exigirMaiuscula", false,
        "exigirMinuscula", false,
        "exigirNumero", false,
        "exigirEspecial", false,
        "quantidadeHistorico", 0,
        "maxTentativasInvalidas", 5));
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/rbac/politica-senha")))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
  }

  private UUID criarCliente(String token, String nome, String slug) throws Exception {
    String body = json.writeValueAsString(Map.of("nome", nome, "slug", slug, "ativo", true));
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/clientes")))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(201);
    return UUID.fromString((String) json.readValue(resp.body(), Map.class).get("id"));
  }

  private UUID criarUsuario(String token, String username, String senha) throws Exception {
    String body = json.writeValueAsString(Map.of(
        "username", username, "password", senha, "nome", username, "email", username + "@x.com"));
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/rbac/usuarios")))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(201);
    return UUID.fromString((String) json.readValue(resp.body(), Map.class).get("id"));
  }

  @SuppressWarnings("unchecked")
  private UUID idDoGrupo(String token, String codigo) throws Exception {
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/rbac/grupos?size=100")))
            .header("Authorization", "Bearer " + token).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    List<Map<String, Object>> items = (List<Map<String, Object>>) json.readValue(resp.body(), Map.class).get("items");
    return items.stream()
        .filter(g -> codigo.equals(g.get("codigo")))
        .map(g -> UUID.fromString((String) g.get("id")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Grupo " + codigo + " não encontrado"));
  }

  private void vincularUsuarioAoGrupo(String token, UUID usuarioId, UUID grupoId) throws Exception {
    String body = json.writeValueAsString(Map.of("grupoIds", List.of(grupoId.toString())));
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/rbac/usuarios/" + usuarioId + "/grupos")))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
  }

  private void criarEscopoUsuarioReadWrite(String token, UUID usuarioId, UUID clienteId) throws Exception {
    criarEscopo(token, usuarioId, clienteId, false);
  }

  private void criarEscopoUsuarioReadOnly(String token, UUID usuarioId, UUID clienteId) throws Exception {
    criarEscopo(token, usuarioId, clienteId, true);
  }

  private void criarEscopo(String token, UUID usuarioId, UUID clienteId, boolean somenteLeitura)
      throws Exception {
    String body = json.writeValueAsString(Map.of(
        "usuarioId", usuarioId.toString(),
        "clienteId", clienteId.toString(),
        "somenteLeitura", somenteLeitura,
        "ativo", true));
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/rbac/escopos")))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(201);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> listarClientes(String token) throws Exception {
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url("/api/v1/docflow/clientes?size=100")))
            .header("Authorization", "Bearer " + token).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(resp.statusCode()).isEqualTo(200);
    return (List<Map<String, Object>>) json.readValue(resp.body(), Map.class).get("items");
  }

  private int statusGet(String token, String path) throws Exception {
    HttpResponse<String> resp = http.send(
        HttpRequest.newBuilder(URI.create(url(path)))
            .header("Authorization", "Bearer " + token).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    return resp.statusCode();
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
