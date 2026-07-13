package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.entity.HistoricoSenha;
import br.com.softon.rbac.entity.PoliticaSenha;
import br.com.softon.rbac.repository.HistoricoSenhaRepository;
import br.com.softon.rbac.repository.PoliticaSenhaRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import java.lang.reflect.Constructor;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PoliticaSenhaServiceTest {

  @Mock PoliticaSenhaRepository politicaRepository;
  @Mock HistoricoSenhaRepository historicoRepository;
  @Mock AuditoriaService auditoriaService;
  @Mock PasswordEncoder passwordEncoder;

  PoliticaSenhaService service;
  PoliticaSenha politica;
  Principal principal;

  @BeforeEach
  void setUp() throws Exception {
    service = new PoliticaSenhaService(politicaRepository, historicoRepository, auditoriaService, passwordEncoder);
    Constructor<PoliticaSenha> ctor = PoliticaSenha.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    politica = ctor.newInstance();
    principal = () -> "admin";
    when(politicaRepository.findById(PoliticaSenha.SINGLETON_ID)).thenReturn(Optional.of(politica));
  }

  @Test
  void atual_falhaSePoliticaNaoInicializada() {
    when(politicaRepository.findById(PoliticaSenha.SINGLETON_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.atual())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("seed V13");
  }

  @Test
  void atualizar_persisteMudancasEAudita() {
    PoliticaSenha atualizada = service.atualizar(12, true, true, true, true, 90, 5, 3, principal);

    assertThat(atualizada.getTamanhoMinimo()).isEqualTo(12);
    assertThat(atualizada.isExigirEspecial()).isTrue();
    assertThat(atualizada.getExpiraSenhaDias()).isEqualTo(90);
    assertThat(atualizada.getQuantidadeHistorico()).isEqualTo(5);
    assertThat(atualizada.getMaxTentativasInvalidas()).isEqualTo(3);
    verify(auditoriaService).registrar(eq("PoliticaSenha"), any(), eq("EDITAR"), any(), eq(principal));
  }

  @Test
  void validar_semViolacoesQuandoSenhaAtendeATudo() {
    assertThat(service.validar("Abc12345", politica)).isEmpty();
  }

  @Test
  void validar_apontaTamanhoMinimo() {
    List<String> v = service.validar("Ab1", politica);

    assertThat(v).anyMatch(s -> s.contains("Mínimo"));
  }

  @Test
  void validar_apontaMaiuscula() {
    politica.atualizar(4, true, false, false, false, null, 0, 5);
    List<String> v = service.validar("abcd", politica);

    assertThat(v).anyMatch(s -> s.contains("maiúscula"));
  }

  @Test
  void validar_apontaMinuscula() {
    politica.atualizar(4, false, true, false, false, null, 0, 5);
    List<String> v = service.validar("ABCD", politica);

    assertThat(v).anyMatch(s -> s.contains("minúscula"));
  }

  @Test
  void validar_apontaNumero() {
    politica.atualizar(4, false, false, true, false, null, 0, 5);
    List<String> v = service.validar("abcd", politica);

    assertThat(v).anyMatch(s -> s.contains("número"));
  }

  @Test
  void validar_apontaEspecial() {
    politica.atualizar(4, false, false, false, true, null, 0, 5);
    List<String> v = service.validar("abcd", politica);

    assertThat(v).anyMatch(s -> s.contains("especial"));
  }

  @Test
  void validar_senhaNulaDisparaTodasAsViolacoes() {
    List<String> v = service.validar(null, politica);

    assertThat(v).hasSize(4); // tamanho + maiuscula + minuscula + numero (especial=false por padrão)
  }

  @Test
  void validarOuFalhar_lancaBusinessQuandoInvalida() {
    assertThatThrownBy(() -> service.validarOuFalhar("abc"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Senha não atende");
  }

  @Test
  void validarOuFalhar_naoLancaQuandoValida() {
    service.validarOuFalhar("Abc12345");
  }

  @Test
  void reutilizada_falseQuandoHistoricoDesabilitado() {
    politica.atualizar(8, true, true, true, false, null, 0, 5);
    UUID uid = UUID.randomUUID();

    assertThat(service.reutilizada(uid, "qualquerCoisa")).isFalse();
  }

  @Test
  void reutilizada_trueQuandoSenhaBateEmAlgumHash() {
    UUID uid = UUID.randomUUID();
    HistoricoSenha h1 = new HistoricoSenha(uid, "hash-antigo");
    HistoricoSenha h2 = new HistoricoSenha(uid, "hash-atual");
    when(historicoRepository.findByUsuarioIdOrderByCreatedAtDesc(uid))
        .thenReturn(List.of(h1, h2));
    when(passwordEncoder.matches("nova", "hash-antigo")).thenReturn(false);
    when(passwordEncoder.matches("nova", "hash-atual")).thenReturn(true);

    assertThat(service.reutilizada(uid, "nova")).isTrue();
  }

  @Test
  void reutilizada_falseQuandoSenhaNaoBateEmNenhum() {
    UUID uid = UUID.randomUUID();
    when(historicoRepository.findByUsuarioIdOrderByCreatedAtDesc(uid))
        .thenReturn(List.of(new HistoricoSenha(uid, "h1"), new HistoricoSenha(uid, "h2")));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    assertThat(service.reutilizada(uid, "nova")).isFalse();
  }

  @Test
  void registrarNoHistorico_gravaESemExcedenteNaoDeleta() {
    UUID uid = UUID.randomUUID();
    when(historicoRepository.findByUsuarioIdOrderByCreatedAtDesc(uid))
        .thenReturn(List.of(new HistoricoSenha(uid, "h1")));

    service.registrarNoHistorico(uid, "novo-hash");

    verify(historicoRepository).save(any(HistoricoSenha.class));
  }

  @Test
  void registrarNoHistorico_removeExcedentesAlemDoLimite() {
    UUID uid = UUID.randomUUID();
    List<HistoricoSenha> todos = List.of(
        new HistoricoSenha(uid, "h1"),
        new HistoricoSenha(uid, "h2"),
        new HistoricoSenha(uid, "h3"),
        new HistoricoSenha(uid, "h4"));
    when(historicoRepository.findByUsuarioIdOrderByCreatedAtDesc(uid)).thenReturn(todos);

    service.registrarNoHistorico(uid, "novo-hash");

    // quantidadeHistorico default = 3, excede em 1 → deleta os últimos (subList(3, 4))
    verify(historicoRepository).deleteAll(todos.subList(3, 4));
  }
}
