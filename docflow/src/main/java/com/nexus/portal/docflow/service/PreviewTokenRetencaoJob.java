package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.repository.PreviewTokenRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remove tokens de preview já expirados. Sem isto a tabela só cresce: o token
 * perde a validade mas nunca sai do banco.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewTokenRetencaoJob {

  private final PreviewTokenRepository previewTokenRepository;

  @Value("${docflow.preview.retencao-tokens-dias:30}")
  private int retencaoDias;

  @Scheduled(cron = "${docflow.preview.retencao-cron:0 45 3 * * *}")
  @Transactional
  public void executar() {
    if (retencaoDias <= 0) {
      log.debug("Retenção de tokens de preview desabilitada (dias={}).", retencaoDias);
      return;
    }
    OffsetDateTime limite = OffsetDateTime.now().minusDays(retencaoDias);
    long removidos = previewTokenRepository.deleteByExpiresAtBefore(limite);
    if (removidos > 0) {
      log.info("Retenção de preview removeu {} tokens expirados antes de {}.", removidos, limite);
    }
  }
}
