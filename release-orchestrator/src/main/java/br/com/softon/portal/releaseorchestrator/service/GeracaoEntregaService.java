package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import br.com.softon.portal.releaseorchestrator.integration.publish.PublishException;
import br.com.softon.portal.releaseorchestrator.integration.publish.PublishService;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorConfigEntregaRepository;
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
  private final OrchestratorConfigEntregaRepository configEntregaRepository;
  private final PublishService publishService;

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
      return;
    }
    if (entrega.getStatus() != StatusEntrega.EM_GERACAO) {
      log.warn("Entrega {} não está em EM_GERACAO ({}), abortando geração",
          entregaId, entrega.getStatus());
      return;
    }

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

      publicarSeRemoto(entrega, Path.of(pacote.caminho()));
      atualizarVersoesAtuaisDoCliente(entrega);
      entrega.marcarConcluida(pacote.caminho(), pacote.sha256(), pacote.tamanhoBytes());
      log.info("Entrega {} concluída ({} itens, {} bytes, sha256={})",
          entregaId, pacote.totalItens(), pacote.tamanhoBytes(), pacote.sha256());
    } catch (IOException | RuntimeException ex) {
      log.error("Falha gerando entrega {}: {}", entregaId, ex.getMessage(), ex);
      entrega.marcarFalha(ex.getMessage());
    }
  }

  /**
   * Quando a config de entrega do cliente aponta para FTP/SFTP, dispara o
   * upload do pacote logo após empacotar. PASTA é destino local (pacote já
   * está no disco final), então é noop. Falha de publish lança
   * {@link PublishException} — a entrega é marcada como FALHA pelo catch
   * geral em {@link #executar(UUID)}.
   */
  private void publicarSeRemoto(Entrega entrega, Path pacote) {
    var config = configEntregaRepository
        .findByCliente_Id(entrega.getCliente().getId())
        .orElse(null);
    if (config == null || config.getTipoDestino() == TipoDestinoEntrega.PASTA) {
      return;
    }
    var resultado = publishService.publicar(config, pacote);
    log.info("Pacote publicado em {} ({} bytes)",
        resultado.destino(), resultado.tamanhoBytes());
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
