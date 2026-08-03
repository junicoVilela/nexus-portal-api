package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job de retenção de pacotes ZIP (F4 fase 2). Roda diariamente às 03:00
 * (horário do servidor) e apaga do disco todo pacote de entrega CONCLUIDA
 * mais velho que {@code release-orchestrator.storage.retencao-dias} dias.
 *
 * <p>Estratégia:
 * <ul>
 *   <li>Critério temporal: {@link Entrega#getDataConclusao()} <
 *       agora - retencaoDias.</li>
 *   <li>Apenas entregas CONCLUIDA com {@code arquivoPacoteCaminho} não null
 *       — entregas em outros status ou já descartadas não entram.</li>
 *   <li>SHA-256 é preservado em {@code arquivoPacoteSha256} para auditoria
 *       (sabemos o que foi entregue, mesmo sem o ZIP).</li>
 *   <li>Falha ao apagar arquivo individual não interrompe o job; só vira
 *       log.warn — a próxima execução tenta de novo.</li>
 * </ul>
 *
 * <p>Quando {@code retencao-dias <= 0}, o job é noop (retenção desligada).
 *
 * <p>Decisão de deploy: storage local (memo
 * {@code storage-disco-local-servidor.md}). Sem coordenação distribuída.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetencaoPacotesJob {

  private final OrchestratorEntregaRepository entregaRepository;
  private final ReleaseOrchestratorStorageProperties storage;

  /** Diariamente às 03:00 — janela tranquila no horário comercial brasileiro. */
  @Scheduled(cron = "${release-orchestrator.storage.retencao-cron:0 0 3 * * *}")
  @Transactional
  public void executar() {
    int dias = storage.retencaoDias();
    if (dias <= 0) {
      log.debug("Retenção desabilitada (retencao-dias={}).", dias);
      return;
    }
    OffsetDateTime cutoff = OffsetDateTime.now().minusDays(dias);
    log.info("Iniciando retenção de pacotes mais antigos que {} dias (cutoff={}).", dias, cutoff);

    List<Entrega> candidatas = entregaRepository
        .findByStatusAndArquivoPacoteCaminhoNotNullAndDataConclusaoBefore(
            StatusEntrega.CONCLUIDA, cutoff);
    if (candidatas.isEmpty()) {
      log.info("Nenhum pacote para descartar nesta janela.");
      return;
    }

    int apagados = 0;
    long bytesLiberados = 0;
    int erros = 0;
    for (Entrega entrega : candidatas) {
      String caminho = entrega.getArquivoPacoteCaminho();
      Path arquivo = Path.of(caminho);
      try {
        long tamanho = Files.exists(arquivo) ? Files.size(arquivo) : 0;
        Files.deleteIfExists(arquivo);
        entrega.marcarPacoteDescartado();
        apagados++;
        bytesLiberados += tamanho;
        log.info("Pacote descartado: entrega={} cliente={} caminho={} bytes={}",
            entrega.getId(), entrega.getCliente().getSigla(), caminho, tamanho);
      } catch (IOException e) {
        erros++;
        log.warn("Falha ao apagar {} (entrega={}): {} — manterá registro pra próxima janela.",
            caminho, entrega.getId(), e.getMessage());
      }
    }
    log.info("Retenção concluída: candidatas={} apagados={} erros={} bytesLiberados={}",
        candidatas.size(), apagados, erros, bytesLiberados);
  }
}
