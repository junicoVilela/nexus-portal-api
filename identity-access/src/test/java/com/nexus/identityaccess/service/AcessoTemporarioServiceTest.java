package com.nexus.identityaccess.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.entity.AcessoTemporario;
import com.nexus.identityaccess.repository.AcessoTemporarioRepository;
import com.nexus.identityaccess.service.AcessoTemporarioService.AcessoTemporarioFilter;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.time.OffsetDateTime;
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
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcessoTemporarioServiceTest {

  @Mock AcessoTemporarioRepository repository;
  @Mock AuditoriaService auditoriaService;

  AcessoTemporarioService service;

  UUID usuarioId;
  UUID grupoId;
  Principal principal;

  @BeforeEach
  void setUp() {
    service = new AcessoTemporarioService(repository, auditoriaService);
    usuarioId = UUID.randomUUID();
    grupoId = UUID.randomUUID();
    principal = () -> "admin";
    when(repository.save(any(AcessoTemporario.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criar_falhaSeNaoInformarNadaAlemDoUsuario() {
    OffsetDateTime ini = OffsetDateTime.now();
    assertThatThrownBy(() ->
        service.criar(usuarioId, null, null, null, ini, ini.plusHours(1), "x", principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("grupo, permissão ou escopo");
  }

  @Test
  void criar_falhaSeFimAntesDoInicio() {
    OffsetDateTime ini = OffsetDateTime.now();
    assertThatThrownBy(() ->
        service.criar(usuarioId, grupoId, null, null, ini, ini.minusHours(1), "x", principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Janela inválida");
  }

  @Test
  void criar_falhaSeInicioOuFimNulo() {
    OffsetDateTime ini = OffsetDateTime.now();
    assertThatThrownBy(() ->
        service.criar(usuarioId, grupoId, null, null, null, ini.plusHours(1), "x", principal))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() ->
        service.criar(usuarioId, grupoId, null, null, ini, null, "x", principal))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void criar_persisteEAudita() {
    OffsetDateTime ini = OffsetDateTime.now();
    AcessoTemporario a = service.criar(usuarioId, grupoId, null, null,
        ini, ini.plusHours(2), "urgência", principal);

    assertThat(a.getUsuarioId()).isEqualTo(usuarioId);
    assertThat(a.getGrupoId()).isEqualTo(grupoId);
    assertThat(a.getCreatedBy()).isEqualTo("admin");
    verify(auditoriaService).registrar(eq("AcessoTemporario"), any(), eq("CRIAR"), any(), eq(principal));
  }

  @Test
  void criar_usaSystemSePrincipalNulo() {
    OffsetDateTime ini = OffsetDateTime.now();
    AcessoTemporario a = service.criar(usuarioId, grupoId, null, null,
        ini, ini.plusHours(1), null, null);

    assertThat(a.getCreatedBy()).isEqualTo("system");
  }

  @Test
  void revogar_marcaComoRevogadoEAudita() throws Exception {
    UUID id = UUID.randomUUID();
    AcessoTemporario a = novoAtivo();
    setField(a, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(a));

    AcessoTemporario resultado = service.revogar(id, "usuário desligado", principal);

    assertThat(resultado.getRevogadoEm()).isNotNull();
    assertThat(resultado.getMotivoRevogacao()).isEqualTo("usuário desligado");
    verify(auditoriaService).registrar(eq("AcessoTemporario"), eq(id), eq("REVOGAR"), any(), eq(principal));
  }

  @Test
  void revogar_motivoEmBrancoRegistraTextoPadrao() throws Exception {
    UUID id = UUID.randomUUID();
    AcessoTemporario a = novoAtivo();
    setField(a, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(a));

    AcessoTemporario resultado = service.revogar(id, "  ", principal);

    assertThat(resultado.getMotivoRevogacao()).isEqualTo("Revogado manualmente.");
  }

  @Test
  void revogar_falhaSeJaRevogado() throws Exception {
    UUID id = UUID.randomUUID();
    AcessoTemporario a = novoAtivo();
    setField(a, "id", id);
    a.revogar("já foi");
    when(repository.findById(id)).thenReturn(Optional.of(a));

    assertThatThrownBy(() -> service.revogar(id, "outra vez", principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já revogado");
  }

  @Test
  void revogar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.revogar(id, "x", principal))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void listar_delegaComFilterVazio() {
    service.listar(new AcessoTemporarioFilter(null, null), PageRequest.of(0, 10));

    verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
        any(org.springframework.data.domain.Pageable.class));
  }

  @Test
  void gruposAtivosDoUsuario_deduplica() throws Exception {
    AcessoTemporario a1 = new AcessoTemporario(usuarioId, grupoId, null, null,
        OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "", "system");
    AcessoTemporario a2 = new AcessoTemporario(usuarioId, grupoId, null, null,
        OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "", "system");
    when(repository.findAtivosDoUsuario(eq(usuarioId), any(OffsetDateTime.class)))
        .thenReturn(List.of(a1, a2));

    assertThat(service.gruposAtivosDoUsuario(usuarioId)).containsExactly(grupoId);
  }

  @Test
  void gruposAtivosDoUsuario_ignoraSemGrupo() {
    AcessoTemporario semGrupo = new AcessoTemporario(usuarioId, null, UUID.randomUUID(), null,
        OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "", "system");
    when(repository.findAtivosDoUsuario(eq(usuarioId), any(OffsetDateTime.class)))
        .thenReturn(List.of(semGrupo));

    assertThat(service.gruposAtivosDoUsuario(usuarioId)).isEmpty();
  }

  private AcessoTemporario novoAtivo() {
    OffsetDateTime ini = OffsetDateTime.now();
    return new AcessoTemporario(usuarioId, grupoId, null, null,
        ini, ini.plusHours(2), "urgência", "system");
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
