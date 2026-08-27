package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.config.PublicacaoProperties;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicacaoWatchdogJobTest {

  @Mock PublicacaoRepository publicacaoRepository;
  @Mock PublicacaoEventService publicacaoEventService;

  PublicacaoWatchdogJob job;

  @BeforeEach
  void setUp() {
    job = new PublicacaoWatchdogJob(publicacaoRepository, publicacaoEventService,
        new PublicacaoProperties(30));
  }

  @Test
  void reconciliar_publicacaoPresaEmGerando_viraErroEAvisaOsAssinantes() {
    Publicacao presa = new Publicacao(new Cliente("ACME", "acme", true), "1.0.0", null);
    when(publicacaoRepository.findByStatusAndUpdatedAtBefore(eq(StatusPublicacao.GERANDO), any()))
        .thenReturn(List.of(presa));

    job.reconciliar();

    assertThat(presa.getStatus()).isEqualTo(StatusPublicacao.ERRO);
    assertThat(presa.getObservacao()).contains("Geração interrompida");
    verify(publicacaoEventService).publicar(presa);
  }

  @Test
  void reconciliar_usaOTimeoutConfigurado() {
    job = new PublicacaoWatchdogJob(publicacaoRepository, publicacaoEventService,
        new PublicacaoProperties(90));
    when(publicacaoRepository.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(List.of());
    OffsetDateTime antes = OffsetDateTime.now().minusMinutes(90);

    job.reconciliar();

    ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(publicacaoRepository)
        .findByStatusAndUpdatedAtBefore(eq(StatusPublicacao.GERANDO), captor.capture());
    assertThat(captor.getValue()).isBetween(antes.minusSeconds(5), antes.plusSeconds(5));
  }

  @Test
  void reconciliar_semPublicacoesPresas_naoPublicaEvento() {
    when(publicacaoRepository.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(List.of());

    job.reconciliar();

    verifyNoInteractions(publicacaoEventService);
  }
}
