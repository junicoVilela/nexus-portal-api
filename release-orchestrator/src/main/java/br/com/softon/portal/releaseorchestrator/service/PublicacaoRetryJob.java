package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.StatusPublicacao;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Job de retry de publicação remota (F3 P2). A cada
 * {@code release-orchestrator.publicacao.retry-cron} (default 2 min), varre
 * entregas com status_publicacao = PENDENTE cuja proxima_tentativa_em já
 * venceu e invoca {@link PublicacaoRemotaService#tentar(java.util.UUID)}
 * para cada uma.
 *
 * <p>Cada tentativa roda em sua própria transação — falha de uma não
 * interrompe o loop. O serviço é responsável por backoff e marcação de
 * FALHA definitivo após esgotar {@code max-tentativas}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicacaoRetryJob {

  private final OrchestratorEntregaRepository entregaRepository;
  private final PublicacaoRemotaService publicacaoService;

  @Scheduled(cron = "${release-orchestrator.publicacao.retry-cron:0 */2 * * * *}")
  public void executar() {
    OffsetDateTime agora = OffsetDateTime.now();
    List<Entrega> candidatas = entregaRepository
        .findByStatusPublicacaoAndProximaTentativaEmLessThanEqual(
            StatusPublicacao.PENDENTE, agora);
    if (candidatas.isEmpty()) {
      log.debug("Nenhuma publicação pendente vencida em {}.", agora);
      return;
    }
    log.info("Publicação retry job: {} candidata(s).", candidatas.size());
    for (Entrega entrega : candidatas) {
      try {
        publicacaoService.tentar(entrega.getId());
      } catch (RuntimeException e) {
        // Defesa: tentar() já encapsula seu próprio erro; chegar aqui é raro.
        log.error("Erro inesperado no retry da entrega {}: {}",
            entrega.getId(), e.getMessage(), e);
      }
    }
  }
}
