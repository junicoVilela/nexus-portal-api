package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.config.PublicacaoProperties;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconcilia publicações que ficaram presas em {@code GERANDO}. Sem isto, uma
 * queda do processo no meio da geração deixa a publicação em um estado que a UI
 * não consegue reprocessar nem excluir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicacaoWatchdogJob {

  private final PublicacaoRepository publicacaoRepository;
  private final PublicacaoEventService publicacaoEventService;
  private final PublicacaoProperties properties;

  @Scheduled(cron = "${docflow.publicacao.watchdog-cron:0 */5 * * * *}")
  @Transactional
  public void reconciliar() {
    OffsetDateTime limite = OffsetDateTime.now().minusMinutes(properties.timeoutMinutos());
    List<Publicacao> presas =
        publicacaoRepository.findByStatusAndUpdatedAtBefore(StatusPublicacao.GERANDO, limite);
    for (Publicacao publicacao : presas) {
      publicacao.registrarErro("Geração interrompida: sem conclusão em "
          + properties.timeoutMinutos() + " minutos. Reprocesse a publicação.");
      publicacaoEventService.publicar(publicacao);
      log.warn("Publicação {} estava em GERANDO desde {} e foi marcada como ERRO",
          publicacao.getId(), publicacao.getUpdatedAt());
    }
  }
}
