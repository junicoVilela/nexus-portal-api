package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.entity.Sessao;
import br.com.softon.rbac.repository.SessaoRepository;
import br.com.softon.rbac.service.SessaoService.SessaoFilter;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.time.OffsetDateTime;
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
class SessaoServiceTest {

  @Mock SessaoRepository repository;
  @Mock AuditoriaService auditoriaService;

  SessaoService service;

  Principal principal;
  UUID usuarioId;

  @BeforeEach
  void setUp() {
    service = new SessaoService(repository, auditoriaService);
    principal = () -> "admin";
    usuarioId = UUID.randomUUID();
    when(repository.save(any(Sessao.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void abrir_persisteNovaSessao() {
    Sessao s = service.abrir("jti-x", usuarioId, "127.0.0.1", "Mozilla",
        OffsetDateTime.now().plusHours(1));

    assertThat(s.getJti()).isEqualTo("jti-x");
    assertThat(s.getUsuarioId()).isEqualTo(usuarioId);
    assertThat(s.isAtiva()).isTrue();
  }

  @Test
  void sessaoAtiva_failOpenParaJtiVazio() {
    assertThat(service.sessaoAtiva(null)).isTrue();
    assertThat(service.sessaoAtiva("")).isTrue();
    assertThat(service.sessaoAtiva("   ")).isTrue();
  }

  @Test
  void sessaoAtiva_falseSeInexistente() {
    when(repository.findByJti("miss")).thenReturn(Optional.empty());

    assertThat(service.sessaoAtiva("miss")).isFalse();
  }

  @Test
  void sessaoAtiva_trueSeAtivaNaoExpirada() {
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusMinutes(5));
    when(repository.findByJti("jti")).thenReturn(Optional.of(s));

    assertThat(service.sessaoAtiva("jti")).isTrue();
  }

  @Test
  void sessaoAtiva_falseSeExpirada() {
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().minusMinutes(1));
    when(repository.findByJti("jti")).thenReturn(Optional.of(s));

    assertThat(service.sessaoAtiva("jti")).isFalse();
  }

  @Test
  void sessaoAtiva_falseSeInativa() {
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusHours(1));
    s.revogar("logout");
    when(repository.findByJti("jti")).thenReturn(Optional.of(s));

    assertThat(service.sessaoAtiva("jti")).isFalse();
  }

  @Test
  void sessaoAtiva_trueSeSemExpiracao() {
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", null);
    when(repository.findByJti("jti")).thenReturn(Optional.of(s));

    assertThat(service.sessaoAtiva("jti")).isTrue();
  }

  @Test
  void revogar_marcaEncerraEAudita() throws Exception {
    UUID id = UUID.randomUUID();
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusHours(1));
    setField(s, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(s));

    Sessao resultado = service.revogar(id, "manual", principal);

    assertThat(resultado.isAtiva()).isFalse();
    assertThat(resultado.getMotivoEncerramento()).isEqualTo("manual");
    verify(auditoriaService).registrar(eq("Sessao"), eq(id), eq("REVOGAR"), any(), eq(principal));
  }

  @Test
  void revogar_motivoEmBrancoUsaPadrao() throws Exception {
    UUID id = UUID.randomUUID();
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusHours(1));
    setField(s, "id", id);
    when(repository.findById(id)).thenReturn(Optional.of(s));

    Sessao r = service.revogar(id, null, principal);

    assertThat(r.getMotivoEncerramento()).isEqualTo("Revogada manualmente.");
  }

  @Test
  void revogar_falhaSeJaEncerrada() throws Exception {
    UUID id = UUID.randomUUID();
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusHours(1));
    setField(s, "id", id);
    s.encerrarPorLogout();
    when(repository.findById(id)).thenReturn(Optional.of(s));

    assertThatThrownBy(() -> service.revogar(id, "manual", principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já está encerrada");
  }

  @Test
  void revogar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.revogar(id, "manual", principal))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void encerrarPorLogout_ignoraSeSessaoJaInativa() {
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusHours(1));
    s.revogar("prev");
    OffsetDateTime encerradaAntes = s.getEncerradaEm();
    when(repository.findByJti("jti")).thenReturn(Optional.of(s));

    service.encerrarPorLogout("jti");

    // já estava inativa antes → encerrada não é sobrescrita
    assertThat(s.getEncerradaEm()).isEqualTo(encerradaAntes);
  }

  @Test
  void encerrarPorLogout_encerraSessaoAtiva() {
    Sessao s = new Sessao("jti", usuarioId, "ip", "ua", OffsetDateTime.now().plusHours(1));
    when(repository.findByJti("jti")).thenReturn(Optional.of(s));

    service.encerrarPorLogout("jti");

    assertThat(s.isAtiva()).isFalse();
  }

  @Test
  void encerrarPorLogout_ignoraSeSessaoInexistente() {
    when(repository.findByJti("miss")).thenReturn(Optional.empty());

    service.encerrarPorLogout("miss");

    verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
  }

  @Test
  void listar_delegaAoRepo() {
    service.listar(new SessaoFilter(null, null), PageRequest.of(0, 10));

    verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
        any(org.springframework.data.domain.Pageable.class));
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
