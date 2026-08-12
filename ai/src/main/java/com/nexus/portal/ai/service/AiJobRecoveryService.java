package com.nexus.portal.ai.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobRecoveryService {

  private final AiJobLifecycleService lifecycleService;
  private final AiJobWorkerService workerService;

  @EventListener(ApplicationReadyEvent.class)
  public void recuperar() {
    for (UUID jobId : lifecycleService.recuperarAposReinicio()) {
      log.info("ai.job.recovery jobId={} status=PENDENTE", jobId);
      workerService.processar(jobId);
    }
  }
}
