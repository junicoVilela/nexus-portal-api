package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.entity.EscopoAcesso;
import br.com.softon.rbac.entity.Grupo;
import br.com.softon.rbac.entity.Usuario;
import br.com.softon.rbac.repository.EscopoAcessoRepository;
import br.com.softon.rbac.repository.GrupoRepository;
import br.com.softon.rbac.repository.UsuarioRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscopoResolverTest {

  @Mock UsuarioRepository usuarioRepository;
  @Mock GrupoRepository grupoRepository;
  @Mock EscopoAcessoRepository escopoRepository;

  EscopoResolver resolver;

  UUID usuarioId;
  UUID clienteA;
  UUID clienteB;

  @BeforeEach
  void setUp() {
    resolver = new EscopoResolver(usuarioRepository, grupoRepository, escopoRepository);
    usuarioId = UUID.randomUUID();
    clienteA = UUID.randomUUID();
    clienteB = UUID.randomUUID();
    when(grupoRepository.findAtivosComUsuario(usuarioId)).thenReturn(List.of());
  }

  @AfterEach
  void limparContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void clientesPermitidos_semUsuario_retornaEmpty() {
    assertThat(resolver.clientesPermitidos(null)).isEmpty();
  }

  @Test
  void clientesPermitidos_semEscopos_retornaEmptyIndicandoSemRestricao() {
    when(escopoRepository.findByUsuarioIdAndAtivoTrue(usuarioId)).thenReturn(List.of());

    assertThat(resolver.clientesPermitidos(usuarioId)).isEmpty();
  }

  @Test
  void clientesPermitidos_apenasEscoposSemCliente_retornaEmptyIndicandoSemRestricao() {
    when(escopoRepository.findByUsuarioIdAndAtivoTrue(usuarioId))
        .thenReturn(List.of(escopo(null)));

    assertThat(resolver.clientesPermitidos(usuarioId)).isEmpty();
  }

  @Test
  void clientesPermitidos_uneCliententesDeEscoposDoUsuarioEDosGrupos() throws Exception {
    Grupo grupo = grupo(UUID.randomUUID());
    when(escopoRepository.findByUsuarioIdAndAtivoTrue(usuarioId))
        .thenReturn(List.of(escopo(clienteA)));
    when(grupoRepository.findAtivosComUsuario(usuarioId)).thenReturn(List.of(grupo));
    when(escopoRepository.findByGrupoIdAndAtivoTrue(grupo.getId()))
        .thenReturn(List.of(escopo(clienteB)));

    assertThat(resolver.clientesPermitidos(usuarioId))
        .hasValueSatisfying(set -> assertThat(set).containsExactlyInAnyOrder(clienteA, clienteB));
  }

  @Test
  void clientesPermitidosDoUsuarioAtual_semAuth_retornaEmpty() {
    assertThat(resolver.clientesPermitidosDoUsuarioAtual()).isEmpty();
  }

  @Test
  void clientesPermitidosDoUsuarioAtual_usuarioInexistente_retornaEmpty() {
    autenticarComo("fantasma");
    when(usuarioRepository.findByUsernameAndAtivoTrue("fantasma")).thenReturn(Optional.empty());

    assertThat(resolver.clientesPermitidosDoUsuarioAtual()).isEmpty();
  }

  @Test
  void podeAcessarCliente_semRestricao_true() {
    assertThat(resolver.podeAcessarCliente(clienteA)).isTrue();
  }

  @Test
  void podeAcessarCliente_dentroDaWhitelist_true() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of(escopo(clienteA, false)));

    assertThat(resolver.podeAcessarCliente(clienteA)).isTrue();
  }

  @Test
  void podeAcessarCliente_foraDaWhitelist_false() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of(escopo(clienteA, false)));

    assertThat(resolver.podeAcessarCliente(clienteB)).isFalse();
  }

  @Test
  void podeEscreverEmCliente_semAuth_true() {
    assertThat(resolver.podeEscreverEmCliente(clienteA)).isTrue();
  }

  @Test
  void podeEscreverEmCliente_semEscopos_true() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of());

    assertThat(resolver.podeEscreverEmCliente(clienteA)).isTrue();
  }

  @Test
  void podeEscreverEmCliente_apenasEscoposSemCliente_true() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of(escopo(null, false)));

    assertThat(resolver.podeEscreverEmCliente(clienteA)).isTrue();
  }

  @Test
  void podeEscreverEmCliente_clienteForaDaWhitelist_false() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of(escopo(clienteA, false)));

    assertThat(resolver.podeEscreverEmCliente(clienteB)).isFalse();
  }

  @Test
  void podeEscreverEmCliente_clienteApenasComSomenteLeitura_false() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of(escopo(clienteA, true)));

    assertThat(resolver.podeEscreverEmCliente(clienteA)).isFalse();
  }

  @Test
  void podeEscreverEmCliente_pelosMenosUmEscopoDeEscrita_true() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin",
        List.of(escopo(clienteA, true), escopo(clienteA, false)));

    assertThat(resolver.podeEscreverEmCliente(clienteA)).isTrue();
  }

  @Test
  void assertPodeEscreverEmCliente_lancaQuandoNaoPode() throws Exception {
    autenticarUsuarioComEscopos(usuarioId, "admin", List.of(escopo(clienteA, true)));

    assertThatThrownBy(() -> resolver.assertPodeEscreverEmCliente(clienteA))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("somente leitura");
  }

  private void autenticarComo(String username) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(username, null));
  }

  private void autenticarUsuarioComEscopos(UUID id, String username, List<EscopoAcesso> escopos)
      throws Exception {
    autenticarComo(username);
    Usuario u = new Usuario(username, "hash", "Nome", "e@e");
    setField(u, "id", id);
    when(usuarioRepository.findByUsernameAndAtivoTrue(username)).thenReturn(Optional.of(u));
    when(escopoRepository.findByUsuarioIdAndAtivoTrue(id)).thenReturn(escopos);
  }

  private EscopoAcesso escopo(UUID clienteId) {
    return escopo(clienteId, false);
  }

  private EscopoAcesso escopo(UUID clienteId, boolean somenteLeitura) {
    return new EscopoAcesso(null, null, clienteId, null, null, null, somenteLeitura, true);
  }

  private Grupo grupo(UUID id) throws Exception {
    Grupo g = new Grupo("CODIGO", "Nome", "", true);
    setField(g, "id", id);
    return g;
  }

  private static void setField(Object entity, String name, Object value) throws Exception {
    Class<?> c = entity.getClass();
    while (c != null) {
      try {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        f.set(entity, value);
        return;
      } catch (NoSuchFieldException ignore) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }
}
