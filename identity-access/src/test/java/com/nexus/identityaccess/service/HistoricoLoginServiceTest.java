package com.nexus.identityaccess.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.entity.HistoricoLogin;
import com.nexus.identityaccess.repository.HistoricoLoginRepository;
import com.nexus.identityaccess.service.HistoricoLoginService.HistoricoLoginFilter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HistoricoLoginServiceTest {

  @Mock HistoricoLoginRepository repository;

  HistoricoLoginService service;

  @BeforeEach
  void setUp() {
    service = new HistoricoLoginService(repository);
  }

  @Test
  void registrar_persisteRegistro() {
    UUID uid = UUID.randomUUID();

    service.registrar(uid, "admin", "127.0.0.1", "Mozilla", true, null);

    ArgumentCaptor<HistoricoLogin> captor = ArgumentCaptor.forClass(HistoricoLogin.class);
    verify(repository).save(captor.capture());
    HistoricoLogin salvo = captor.getValue();
    assertThat(salvo.getUsuarioId()).isEqualTo(uid);
    assertThat(salvo.getLoginInformado()).isEqualTo("admin");
    assertThat(salvo.getIpOrigem()).isEqualTo("127.0.0.1");
    assertThat(salvo.isSucesso()).isTrue();
  }

  @Test
  void registrar_gravaMotivoFalha() {
    service.registrar(null, "hacker", "1.2.3.4", "curl", false, "senha inválida");

    ArgumentCaptor<HistoricoLogin> captor = ArgumentCaptor.forClass(HistoricoLogin.class);
    verify(repository).save(captor.capture());
    HistoricoLogin salvo = captor.getValue();
    assertThat(salvo.isSucesso()).isFalse();
    assertThat(salvo.getMotivoFalha()).isEqualTo("senha inválida");
    assertThat(salvo.getUsuarioId()).isNull();
  }

  @Test
  void listar_delegaAoRepoComEspecificacao() {
    Pageable p = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    var filter = new HistoricoLoginFilter(null, null, null, null, null);
    assertThat(service.listar(filter, p).getContent()).isEmpty();

    verify(repository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void listar_aceitaTodosOsFiltros() {
    Pageable p = PageRequest.of(0, 10);
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    var filter = new HistoricoLoginFilter(
        UUID.randomUUID(), "admin", true,
        OffsetDateTime.now().minusDays(1), OffsetDateTime.now());
    service.listar(filter, p);

    verify(repository).findAll(any(Specification.class), any(Pageable.class));
  }
}
