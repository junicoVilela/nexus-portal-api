package com.nexus.portal;

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
 * Sobe Postgres efêmero, aplica todas as migrations e valida que os 4 usuários
 * seed conseguem logar e que o /auth/me devolve o grupo RBAC esperado.
 *
 * <p>Distribuição esperada por grupo após V5/V8/V9:
 * <ul>
 *   <li><b>ADMIN</b>: todas as permissões ativas do catálogo (SEGURANCA, SISTEMA,
 *       DOC_FLOW, RELEASE_ORCHESTRATOR).</li>
 *   <li><b>EDITOR</b>: CRUD operacional em SEGURANCA (USUARIO/GRUPO_ACESSO/
 *       ACESSO_TEMPORARIO) + ações especiais, CRUD nas funcionalidades de
 *       DOC_FLOW (CLIENTE/PROJETO/MODULO/PAGINA/PUBLICACAO/EMPRESA) e
 *       RELEASE_ORCHESTRATOR (RELEASE/PRODUTO/TEMPLATE/CLIENTE_RO/ENTREGA/
 *       PROXIMA_ENTREGA).</li>
 *   <li><b>REVISOR</b>: :LER em todas as funcionalidades + VISUALIZAR de
 *       histórico/auditoria.</li>
 *   <li><b>LEITOR</b>: somente :LER em todas as funcionalidades.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthSeedsIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

  @Value("${local.server.port}")
  int port;

  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void admin_loga_e_pertence_ao_grupo_ADMIN() throws Exception {
    Map<String, Object> me = loginEBuscarMe("admin", "admin");
    assertThat(grupos(me)).containsExactly("ADMIN");
    // V5(SEGURANCA+SISTEMA): 55 ~ V8(+DOC_FLOW+RELEASE): +32 ~ V9(+EMPRESA+CLIENTE_RO+ENTREGA+PROXIMA_ENTREGA): +16
    assertThat(permissoes(me)).hasSizeGreaterThan(95);
    // Sanity: catálogo SEGURANCA + DOC_FLOW + RELEASE_ORCHESTRATOR coberto
    assertThat(permissoes(me)).contains(
        "USUARIO:LER", "GRUPO_ACESSO:LER",
        "CLIENTE:LER", "PROJETO:LER", "PUBLICACAO:CRIAR",
        "RELEASE:LER", "PRODUTO:LER", "TEMPLATE:LER",
        "EMPRESA:EDITAR", "CLIENTE_RO:LER", "ENTREGA:CRIAR", "PROXIMA_ENTREGA:LER");
  }

  @Test
  void editor_loga_e_faz_gestao_operacional_do_modulo_seguranca() throws Exception {
    Map<String, Object> me = loginEBuscarMe("editor", "editor");
    assertThat(grupos(me)).containsExactly("EDITOR");
    // CRUD em USUARIO/GRUPO_ACESSO/ACESSO_TEMPORARIO
    assertThat(permissoes(me)).contains(
        "USUARIO:CRIAR", "USUARIO:EDITAR", "USUARIO:EXCLUIR",
        "GRUPO_ACESSO:CRIAR", "GRUPO_ACESSO:EDITAR",
        "ACESSO_TEMPORARIO:CRIAR", "ACESSO_TEMPORARIO:EDITAR");
    // Ações especiais que o operador executa
    assertThat(permissoes(me)).contains(
        "USUARIO:RESETAR_SENHA", "USUARIO:BLOQUEAR",
        "GRUPO_ACESSO:VINCULAR_PERMISSAO",
        "SESSAO:REVOGAR", "ACESSO_TEMPORARIO:REVOGAR");
    // EDITAR em POLITICA_SENHA
    assertThat(permissoes(me)).contains("POLITICA_SENHA:EDITAR");
    // CRUD nas funcionalidades de DOC_FLOW e RELEASE_ORCHESTRATOR
    assertThat(permissoes(me)).contains(
        "CLIENTE:CRIAR", "PROJETO:CRIAR", "MODULO:CRIAR",
        "PAGINA:CRIAR", "PUBLICACAO:CRIAR",
        "RELEASE:CRIAR", "PRODUTO:CRIAR", "TEMPLATE:CRIAR",
        "ENTREGA:CRIAR", "PROXIMA_ENTREGA:CRIAR", "CLIENTE_RO:CRIAR");
    // Mas NÃO mexe no catálogo RBAC nem visualiza auditoria/histórico
    assertThat(permissoes(me))
        .doesNotContain("DOMINIO:CRIAR", "FUNCIONALIDADE:CRIAR", "PERMISSAO:CRIAR")
        .doesNotContain("AUDITORIA:VISUALIZAR", "HISTORICO_LOGIN:VISUALIZAR");
  }

  @Test
  void revisor_loga_e_so_le_o_catalogo() throws Exception {
    Map<String, Object> me = loginEBuscarMe("revisor", "revisor");
    assertThat(grupos(me)).containsExactly("REVISOR");
    // Vê histórico de login e trilha de auditoria
    assertThat(permissoes(me)).contains(
        "AUDITORIA:VISUALIZAR", "HISTORICO_LOGIN:VISUALIZAR");
    // LER em funcionalidades de todos os domínios
    assertThat(permissoes(me)).contains(
        "USUARIO:LER", "GRUPO_ACESSO:LER", "DOMINIO:LER", "FUNCIONALIDADE:LER",
        "PERMISSAO:LER", "ESCOPO:LER", "AUDITORIA:LER", "HISTORICO_LOGIN:LER",
        "POLITICA_SENHA:LER", "SESSAO:LER", "ACESSO_TEMPORARIO:LER",
        "CONFIGURACAO:LER",
        "CLIENTE:LER", "PROJETO:LER", "PUBLICACAO:LER",
        "RELEASE:LER", "PRODUTO:LER", "ENTREGA:LER", "PROXIMA_ENTREGA:LER");
    // Não cria/edita/exclui nada
    assertThat(permissoes(me))
        .noneMatch(p -> p.endsWith(":CRIAR")
            || p.endsWith(":EDITAR")
            || p.endsWith(":EXCLUIR"));
  }

  @Test
  void leitor_loga_e_pertence_ao_grupo_LEITOR_com_somente_LER() throws Exception {
    Map<String, Object> me = loginEBuscarMe("leitor", "leitor");
    assertThat(grupos(me)).containsExactly("LEITOR");
    assertThat(permissoes(me)).isNotEmpty();
    // Todas as permissões de LEITOR são :LER
    assertThat(permissoes(me)).allMatch(p -> p.endsWith(":LER"));
    // Cobre os 4 domínios
    assertThat(permissoes(me)).contains(
        "USUARIO:LER", "CLIENTE:LER", "RELEASE:LER", "CONFIGURACAO:LER");
  }

  /**
   * Regressão: {@code /rbac/catalogo/funcionalidades} e {@code /permissoes}
   * navegam entidades LAZY (Dominio/Funcionalidade) no mapping; sem
   * {@code @Transactional} no controller o Hibernate lançava
   * {@code LazyInitializationException} e o filtro devolvia 401.
   */
  @Test
  void catalogo_retorna_dominios_funcionalidades_e_permissoes_sem_lazy_init() throws Exception {
    String token = login("admin", "admin");

    for (String path : List.of(
        "/api/v1/rbac/catalogo/dominios",
        "/api/v1/rbac/catalogo/funcionalidades",
        "/api/v1/rbac/catalogo/permissoes")) {
      HttpResponse<String> resp = http.send(
          HttpRequest.newBuilder(URI.create(url(path)))
              .header("Authorization", "Bearer " + token).GET().build(),
          HttpResponse.BodyHandlers.ofString());
      assertThat(resp.statusCode())
          .withFailMessage("Falha em %s → %d: %s", path, resp.statusCode(), resp.body())
          .isEqualTo(200);
    }
  }

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
