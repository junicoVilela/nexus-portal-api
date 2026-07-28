package br.com.softon.portal.docflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.softon.portal.docflow.config.AjudaProperties;
import br.com.softon.portal.docflow.repository.AjudaEventoRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AjudaEventoRetencaoJobTest {

  @Mock private AjudaEventoRepository eventoRepository;

  @Test
  void executar_deveExcluirEventosForaDaRetencao() {
    AjudaEventoRetencaoJob job = new AjudaEventoRetencaoJob(
        eventoRepository, new AjudaProperties(90));

    job.executar();

    verify(eventoRepository).deleteByCreatedAtBefore(any(OffsetDateTime.class));
  }

  @Test
  void executar_comRetencaoDesabilitada_naoDeveExcluir() {
    AjudaEventoRetencaoJob job = new AjudaEventoRetencaoJob(
        eventoRepository, new AjudaProperties(0));

    job.executar();

    verify(eventoRepository, never()).deleteByCreatedAtBefore(any());
  }
}
