package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.entity.AuditoriaEvento;
import br.com.softon.rbac.repository.AuditoriaRepository;
import java.security.Principal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditoriaServiceTest {

  @Mock AuditoriaRepository auditoriaRepository;

  AuditoriaService service;

  @BeforeEach
  void setUp() {
    service = new AuditoriaService(auditoriaRepository);
  }

  @Test
  void registrar_persisteEventoComUsername() {
    Principal p = () -> "editor";
    UUID entId = UUID.randomUUID();

    service.registrar("Cliente", entId, "CRIAR", "criou X", p);

    ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
    verify(auditoriaRepository).save(captor.capture());
    AuditoriaEvento salvo = captor.getValue();
    assertThat(salvo.getEntidade()).isEqualTo("Cliente");
    assertThat(salvo.getEntidadeId()).isEqualTo(entId);
    assertThat(salvo.getAcao()).isEqualTo("CRIAR");
    assertThat(salvo.getDescricao()).isEqualTo("criou X");
    assertThat(salvo.getCreatedBy()).isEqualTo("editor");
  }

  @Test
  void registrar_usaSystemQuandoPrincipalNulo() {
    service.registrar("Cliente", UUID.randomUUID(), "CRIAR", null, null);

    ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
    verify(auditoriaRepository).save(captor.capture());
    assertThat(captor.getValue().getCreatedBy()).isEqualTo("system");
  }

  @Test
  void recentes_delegaAoRepositorio() {
    List<AuditoriaEvento> lista = List.of(new AuditoriaEvento("X", null, "A", "", "u"));
    when(auditoriaRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(lista);

    assertThat(service.recentes()).isSameAs(lista);
  }

  @Test
  void listar_paginadoDelegaAoRepositorio() {
    Page<AuditoriaEvento> page = new PageImpl<>(List.of());
    Pageable p = Pageable.unpaged();
    when(auditoriaRepository.findAll(p)).thenReturn(page);

    assertThat(service.listar(p)).isSameAs(page);
  }
}
