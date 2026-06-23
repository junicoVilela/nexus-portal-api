package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloArtefatoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Geração assíncrona do pacote de entrega (F1.11).
 *
 * Fluxo:
 * <ol>
 *   <li>{@link #iniciar(UUID)} (síncrono): valida pré-requisitos, transiciona
 *       RASCUNHO → EM_GERACAO e dispara {@link #executar(UUID)}.</li>
 *   <li>{@link #executar(UUID)} (@Async): roda em thread separada,
 *       empacota, atualiza ClienteProdutoModulo.versaoAtual e marca a
 *       entrega CONCLUIDA. Em falha, marca FALHA com o motivo.</li>
 * </ol>
 */
@Slf4j
@Service("orchestratorGeracaoEntregaService")
@RequiredArgsConstructor
public class GeracaoEntregaService {

  private final OrchestratorEntregaRepository entregaRepository;
  private final OrchestratorEntregaModuloRepository entregaModuloRepository;
  private final OrchestratorEntregaModuloArtefatoRepository deltaRepository;
  private final OrchestratorClienteProdutoRepository clienteProdutoRepository;
  private final OrchestratorClienteProdutoModuloRepository cpmRepository;
  private final EmpacotadorEntrega empacotador;
  private final RenderizadorFuncionalidades renderizador;
  private final ReleaseOrchestratorStorageProperties storage;
  private final PublicacaoRemotaService publicacaoRemotaService;
  private final MeterRegistry meterRegistry;

  // Métricas F4 — lazy-init via @PostConstruct para garantir registro idempotente.
  private Timer timerGeracao;
  private Counter counterSucesso;
  private Counter counterFalha;
  private Counter counterCancelada;

  @PostConstruct
  void registrarMetricas() {
    this.timerGeracao = Timer.builder("entrega.geracao.duration")
        .description("Tempo total de geração de uma entrega (empacotador + publish + persistência)")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry);
    this.counterSucesso = Counter.builder("entrega.geracao.resultado")
        .description("Total de gerações de entrega por desfecho")
        .tag("status", "sucesso")
        .register(meterRegistry);
    this.counterFalha = Counter.builder("entrega.geracao.resultado")
        .description("Total de gerações de entrega por desfecho")
        .tag("status", "falha")
        .register(meterRegistry);
    this.counterCancelada = Counter.builder("entrega.geracao.resultado")
        .description("Total de gerações de entrega por desfecho")
        .tag("status", "cancelada")
        .register(meterRegistry);
  }

  /**
   * Valida que a entrega tem delta calculado e dispara geração assíncrona.
   * Retorna a entrega já em EM_GERACAO.
   */
  @Transactional
  public Entrega iniciar(UUID entregaId) {
    Entrega entrega = entregaRepository.findById(entregaId)
        .orElseThrow(() -> new NotFoundException("Entrega não encontrada."));
    if (entrega.getStatus() != StatusEntrega.RASCUNHO
        && entrega.getStatus() != StatusEntrega.FALHA) {
      throw new BusinessException(
          "Entrega em " + entrega.getStatus() + " — só RASCUNHO ou FALHA podem iniciar geração.");
    }
    if (storage.entregasDir() == null || storage.entregasDir().isBlank()) {
      throw new BusinessException(
          "Configuração `release-orchestrator.storage.entregas-dir` ausente.");
    }
    long totalDelta = deltaRepository.findByEntrega_Id(entregaId).size();
    long totalRenderizaveis = entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId).stream()
        .filter(em -> em.isSelecionado() && em.getModuloProduto().getTipo().aceitaUploadDeArtefato() == false)
        .count();
    if (totalDelta == 0 && totalRenderizaveis == 0) {
      throw new BusinessException(
          "Nada a entregar. Selecione ao menos um módulo e calcule o delta antes da geração.");
    }
    entrega.marcarEmGeracao();
    executar(entregaId);
    return entrega;
  }

  /**
   * Geração propriamente dita. Não invocar diretamente — use
   * {@link #iniciar(UUID)}, que faz as validações e transações corretas.
   */
  @Async
  @Transactional
  public void executar(UUID entregaId) {
    Entrega entrega = entregaRepository.findById(entregaId).orElse(null);
    if (entrega == null) {
      log.warn("Entrega {} desapareceu antes da geração assíncrona", entregaId);
      counterCancelada.increment();
      return;
    }
    if (entrega.getStatus() != StatusEntrega.EM_GERACAO) {
      log.warn("Entrega {} não está em EM_GERACAO ({}), abortando geração",
          entregaId, entrega.getStatus());
      counterCancelada.increment();
      return;
    }

    // MDC enrichment para logs JSON estruturados (F4).
    MDC.put("entregaId", entregaId.toString());
    MDC.put("clienteId", entrega.getCliente().getId().toString());
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      List<EntregaModuloArtefato> delta = deltaRepository.findByEntrega_Id(entregaId);
      List<EntregaModulo> linhas = entregaModuloRepository
          .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId);
      var virtuais = renderizador.renderizar(entrega, linhas);
      Path destino = Path.of(storage.entregasDir(),
          entrega.getCliente().getSigla().toLowerCase(),
          entrega.getProduto().getSigla().toLowerCase(),
          entrega.getRelease().getVersao());
      var pacote = empacotador.empacotar(entrega, delta, virtuais, destino);

      atualizarVersoesAtuaisDoCliente(entrega);
      entrega.marcarConcluida(pacote.caminho(), pacote.sha256(), pacote.tamanhoBytes());
      publicacaoRemotaService.agendarSeRemoto(entrega);
      log.info("Entrega {} concluída ({} itens, {} bytes, sha256={})",
          entregaId, pacote.totalItens(), pacote.tamanhoBytes(), pacote.sha256());
      counterSucesso.increment();
    } catch (IOException | RuntimeException ex) {
      log.error("Falha gerando entrega {}: {}", entregaId, ex.getMessage(), ex);
      entrega.marcarFalha(ex.getMessage());
      counterFalha.increment();
    } finally {
      sample.stop(timerGeracao);
      MDC.remove("entregaId");
      MDC.remove("clienteId");
    }
  }

  /**
   * Aplica as versões alvo dos módulos selecionados (versaoTo) na tabela de
   * módulos contratados — registra que o cliente agora está naquela versão.
   */
  private void atualizarVersoesAtuaisDoCliente(Entrega entrega) {
    ClienteProduto contrato = clienteProdutoRepository
        .findByCliente_IdAndProduto_Id(entrega.getCliente().getId(),
            entrega.getProduto().getId())
        .orElse(null);
    if (contrato == null) return;

    List<EntregaModulo> linhas = entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entrega.getId());
    for (EntregaModulo em : linhas) {
      if (!em.isSelecionado() || em.getVersaoTo() == null) continue;
      cpmRepository
          .findByClienteProduto_IdAndModuloProduto_Id(contrato.getId(),
              em.getModuloProduto().getId())
          .ifPresent(cpm -> {
            String versaoTo = em.getVersaoTo();
            boolean ativo = cpm.isAtivo();
            cpm.atualizar(versaoTo, ativo);
          });
    }
  }

}
