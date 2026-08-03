package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.config.ReleaseOrchestratorPublicacaoProperties;
import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.entity.StatusPublicacao;
import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import com.nexus.portal.releaseorchestrator.integration.publish.PublishException;
import com.nexus.portal.releaseorchestrator.integration.publish.PublishResult;
import com.nexus.portal.releaseorchestrator.integration.publish.PublishService;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorConfigEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Coordena a publicação remota de uma entrega (F3 P2):
 *
 * <ol>
 *   <li>{@link #agendarSeRemoto(Entrega)} — chamado pelo
 *       {@code GeracaoEntregaService} ao concluir a geração; marca a entrega
 *       como PENDENTE quando o destino é remoto.</li>
 *   <li>{@link #tentar(UUID)} — invocado pelo {@code PublicacaoRetryJob}
 *       para cada candidata. Sucesso → OK; falha → backoff exponencial
 *       (5, 10, 20, 40, 80… min até {@code backoff-max-minutos}).
 *       Ao esgotar {@code max-tentativas}, marca FALHA definitivo.</li>
 *   <li>{@link #reagendar(UUID)} — operador disparou reprocesso manual via UI.</li>
 * </ol>
 *
 * <p>Backoff exponencial puro (sem jitter) — replays distribuídos não são
 * problema com um único nó portal (memo {@code deployment-sem-kubernetes.md}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicacaoRemotaService {

  private final OrchestratorEntregaRepository entregaRepository;
  private final OrchestratorConfigEntregaRepository configRepository;
  private final PublishService publishService;
  private final ReleaseOrchestratorPublicacaoProperties props;

  /**
   * Marca a entrega como PENDENTE de publicação quando o destino é remoto
   * (FTP/SFTP/BUCKET); para PASTA local fica NAO_APLICAVEL.
   * A primeira tentativa é agendada para "agora" — o job na próxima execução
   * já tenta sem esperar o backoff.
   */
  @Transactional
  public void agendarSeRemoto(Entrega entrega) {
    var config = configRepository
        .findByCliente_Id(entrega.getCliente().getId())
        .orElse(null);
    if (config == null || config.getTipoDestino() == TipoDestinoEntrega.PASTA) {
      return;
    }
    entrega.marcarPublicacaoPendente(OffsetDateTime.now());
    log.info("Entrega {} agendada para publicação em {}.",
        entrega.getId(), config.getTipoDestino());
  }

  /**
   * Tenta publicar uma entrega individual. Cada chamada roda em transação
   * própria — o job invoca em loop e falha de uma não afeta as próximas.
   */
  @Transactional
  public void tentar(UUID entregaId) {
    Entrega entrega = entregaRepository.findById(entregaId).orElse(null);
    if (entrega == null) {
      log.warn("Entrega {} desapareceu antes do retry de publicação.", entregaId);
      return;
    }
    if (entrega.getStatusPublicacao() != StatusPublicacao.PENDENTE) {
      log.debug("Entrega {} não está mais PENDENTE ({}), pulando.",
          entregaId, entrega.getStatusPublicacao());
      return;
    }
    if (entrega.getStatus() != StatusEntrega.CONCLUIDA) {
      // Defesa: alguém marcou pra publicar mas geração ainda não terminou.
      log.warn("Entrega {} pendente mas status geração={}; pulando.",
          entregaId, entrega.getStatus());
      return;
    }
    var config = configRepository
        .findByCliente_Id(entrega.getCliente().getId())
        .orElse(null);
    if (config == null) {
      entrega.marcarPublicacaoFalhouDefinitivo(
          "Config de entrega removida — não há destino para publicar.");
      return;
    }
    String caminho = entrega.getArquivoPacoteCaminho();
    if (caminho == null || !Files.exists(Path.of(caminho))) {
      entrega.marcarPublicacaoFalhouDefinitivo(
          "Pacote ZIP não está mais em disco (caminho=" + caminho
              + "). Provavelmente foi descartado pela retenção.");
      return;
    }

    MDC.put("entregaId", entregaId.toString());
    MDC.put("clienteId", entrega.getCliente().getId().toString());
    try {
      PublishResult resultado = publishService.publicar(config, Path.of(caminho));
      entrega.marcarPublicacaoOk(resultado.destino());
      log.info("Entrega {} publicada em {} ({} bytes).",
          entregaId, resultado.destino(), resultado.tamanhoBytes());
    } catch (PublishException e) {
      registrarFalha(entrega, e);
    } catch (RuntimeException e) {
      // Não-categorizada: também conta como tentativa.
      registrarFalha(entrega, new PublishException(e.getMessage(), e));
    } finally {
      MDC.remove("entregaId");
      MDC.remove("clienteId");
    }
  }

  private void registrarFalha(Entrega entrega, PublishException e) {
    int tentativasApos = entrega.getTentativasPublicacao() + 1;
    String motivo = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    if (tentativasApos >= props.maxTentativas()) {
      entrega.marcarPublicacaoFalhouDefinitivo(motivo);
      log.error("Entrega {} esgotou {} tentativas de publicação. Última falha: {}",
          entrega.getId(), tentativasApos, motivo);
      return;
    }
    OffsetDateTime proxima = OffsetDateTime.now()
        .plusMinutes(backoffMinutos(tentativasApos));
    entrega.marcarTentativaPublicacaoFalhou(motivo, proxima);
    log.warn("Entrega {} tentativa {} falhou ({}); próxima em {}.",
        entrega.getId(), tentativasApos, motivo, proxima);
  }

  /** Backoff exponencial limitado por {@code backoff-max-minutos}. */
  private long backoffMinutos(int tentativaNumero) {
    long base = props.backoffInicialMinutos();
    long expoente = Math.max(0, tentativaNumero - 1);
    long minutos = base * (1L << Math.min(expoente, 16));  // cap em 2^16 pra não estourar long
    return Math.min(minutos, props.backoffMaxMinutos());
  }

  /**
   * Operador pediu para reprocessar via UI. Reinicia contagem de tentativas
   * e agenda para a próxima execução do job.
   */
  @Transactional
  public Entrega reagendar(UUID entregaId) {
    Entrega entrega = entregaRepository.findById(entregaId)
        .orElseThrow(() -> new NotFoundException("Entrega não encontrada."));
    if (entrega.getStatus() != StatusEntrega.CONCLUIDA) {
      throw new BusinessException(
          "Só é possível reagendar publicação de entregas CONCLUIDA.");
    }
    if (entrega.getStatusPublicacao() == StatusPublicacao.NAO_APLICAVEL) {
      throw new BusinessException(
          "Esta entrega não tem destino remoto configurado.");
    }
    entrega.reagendarPublicacao();
    log.info("Entrega {} reagendada manualmente para publicação.", entregaId);
    return entrega;
  }
}
