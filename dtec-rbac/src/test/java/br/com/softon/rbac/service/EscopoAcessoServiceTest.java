package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.entity.EscopoAcesso;
import br.com.softon.rbac.entity.EscopoAcesso.TipoAmbiente;
import br.com.softon.rbac.repository.EscopoAcessoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscopoAcessoServiceTest {

  @Mock EscopoAcessoRepository repository;
  @Mock AuditoriaService auditoriaService;

  EscopoAcessoService service;

  UUID usuarioId;
  UUID clienteId;
  Principal principal;

  @BeforeEach
  void setUp() {
    service = new EscopoAcessoService(repository, auditoriaService);
    usuarioId = UUID.randomUUID();
    clienteId = UUID.randomUUID();
    principal = () -> "admin";
    when(repository.save(any(EscopoAcesso.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void listarTodos_delegaAoRepo() {
    List<EscopoAcesso> all = List.of(escopo());
    when(repository.findAll()).thenReturn(all);

    assertThat(service.listarTodos()).isSameAs(all);
  }

  @Test
  void listarPorUsuario_filtraApenasAtivos() {
    List<EscopoAcesso> ativos = List.of(escopo());
    when(repository.findByUsuarioIdAndAtivoTrue(usuarioId)).thenReturn(ativos);

    assertThat(service.listarPorUsuario(usuarioId)).isSameAs(ativos);
  }

  @Test
  void listarPorGrupo_filtraApenasAtivos() {
    UUID grupoId = UUID.randomUUID();
    List<EscopoAcesso> ativos = List.of(escopo());
    when(repository.findByGrupoIdAndAtivoTrue(grupoId)).thenReturn(ativos);

    assertThat(service.listarPorGrupo(grupoId)).isSameAs(ativos);
  }

  @Test
  void buscar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void criar_falhaSeNaoInformarUsuarioNemGrupo() {
    assertThatThrownBy(() ->
        service.criar(null, null, clienteId, null, null, null, false, true, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("usuário ou grupo");
  }

  @Test
  void criar_persisteEAudita() {
    EscopoAcesso e = service.criar(usuarioId, null, clienteId, null, null, "PRD", true, true, principal);

    assertThat(e.getUsuarioId()).isEqualTo(usuarioId);
    assertThat(e.getClienteId()).isEqualTo(clienteId);
    assertThat(e.getTipoAmbiente()).isEqualTo(TipoAmbiente.PRD);
    assertThat(e.isSomenteLeitura()).isTrue();
    verify(auditoriaService).registrar(eq("EscopoAcesso"), any(), eq("CRIAR"), any(), eq(principal));
  }

  @Test
  void criar_aceitaTipoAmbienteEmMinusculas() {
    EscopoAcesso e = service.criar(usuarioId, null, clienteId, null, null, "dev", false, true, principal);

    assertThat(e.getTipoAmbiente()).isEqualTo(TipoAmbiente.DEV);
  }

  @Test
  void criar_ignoraTipoAmbienteBrancoOuNulo() {
    EscopoAcesso e = service.criar(usuarioId, null, clienteId, null, null, "  ", false, true, principal);

    assertThat(e.getTipoAmbiente()).isNull();
  }

  @Test
  void criar_falhaSeTipoAmbienteInvalido() {
    assertThatThrownBy(() ->
        service.criar(usuarioId, null, clienteId, null, null, "QA", false, true, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Tipo de ambiente inválido");
  }

  @Test
  void atualizar_alteraCamposEAudita() throws Exception {
    UUID id = UUID.randomUUID();
    EscopoAcesso existente = escopo();
    setField(existente, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    EscopoAcesso e = service.atualizar(id, clienteId, null, null, "HML", true, false, principal);

    assertThat(e.getClienteId()).isEqualTo(clienteId);
    assertThat(e.getTipoAmbiente()).isEqualTo(TipoAmbiente.HML);
    assertThat(e.isSomenteLeitura()).isTrue();
    assertThat(e.isAtivo()).isFalse();
    verify(auditoriaService).registrar("EscopoAcesso", id, "EDITAR", "Escopo atualizado.", principal);
  }

  @Test
  void alterarStatus_ativarRegistraAcaoAtivar() throws Exception {
    UUID id = UUID.randomUUID();
    EscopoAcesso existente = escopo();
    setField(existente, "id", id);
    existente.setAtivo(false);
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    service.alterarStatus(id, true, principal);

    assertThat(existente.isAtivo()).isTrue();
    verify(auditoriaService).registrar(eq("EscopoAcesso"), eq(id), eq("ATIVAR"), any(), eq(principal));
  }

  @Test
  void alterarStatus_desativarRegistraAcaoDesativar() throws Exception {
    UUID id = UUID.randomUUID();
    EscopoAcesso existente = escopo();
    setField(existente, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    service.alterarStatus(id, false, principal);

    assertThat(existente.isAtivo()).isFalse();
    verify(auditoriaService).registrar(eq("EscopoAcesso"), eq(id), eq("DESATIVAR"), any(), eq(principal));
  }

  @Test
  void remover_deletaEAudita() throws Exception {
    UUID id = UUID.randomUUID();
    EscopoAcesso existente = escopo();
    setField(existente, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    service.remover(id, principal);

    verify(repository).delete(existente);
    verify(auditoriaService).registrar("EscopoAcesso", id, "EXCLUIR", "Escopo removido.", principal);
  }

  private EscopoAcesso escopo() {
    return new EscopoAcesso(usuarioId, null, clienteId, null, null, null, false, true);
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
