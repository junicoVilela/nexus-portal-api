package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.config.AjudaProperties;
import br.com.softon.portal.docflow.repository.AjudaEventoRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AjudaEventoRetencaoJob {

  private final AjudaEventoRepository eventoRepository;
  private final AjudaProperties properties;

  @Scheduled(cron = "${docflow.ajuda.retencao-cron:0 30 3 * * *}")
  @Transactional
  public void executar() {
    int dias = properties.retencaoEventosDias();
    if (dias <= 0) {
      log.debug("Retenção dos eventos de ajuda desabilitada (dias={}).", dias);
      return;
    }
    OffsetDateTime limite = OffsetDateTime.now().minusDays(dias);
    long removidos = eventoRepository.deleteByCreatedAtBefore(limite);
    if (removidos > 0) {
      log.info("Retenção da ajuda removeu {} eventos anteriores a {}.", removidos, limite);
    }
  }
}
