package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.CalcularDeltaRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.DeltaResumoResponse;
import br.com.softon.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubException;
import br.com.softon.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloArtefatoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cálculo de delta MVP (F1.10).
 *
 * MVP (sem Git): para cada {@link EntregaModulo} selecionado,
 * coleta todos os artefatos uploadados manualmente na release+módulo
 * e persiste em {@link EntregaModuloArtefato}. F2.10 vai filtrar via
 * diff Git por tag FROM..TO.
 *
 * Idempotente: pode ser chamado várias vezes em RASCUNHO.
 */
@Service("orchestratorDeltaEntregaService")
@RequiredArgsConstructor
public class DeltaEntregaService {

  private final OrchestratorEntregaModuloArtefatoRepository repository;
  private final OrchestratorEntregaModuloRepository entregaModuloRepository;
  private final ArtefatoReleaseModuloRepository artefatoRepository;
  private final EntregaService entregaService;
  private final GithubAssetSyncService githubAssetSyncService;
  private final GithubDeltaBancoService githubDeltaBancoService;
  private final GithubDeltaKettleService githubDeltaKettleService;

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(DeltaEntregaService.class);

  public List<EntregaModuloArtefato> listar(UUID entregaId) {
    entregaService.buscar(entregaId);
    return repository.findByEntrega_Id(entregaId);
  }

  @Transactional
  public DeltaResumoResponse calcular(UUID entregaId) {
    return calcular(entregaId, null);
  }

  /**
   * Calcula o delta aceitando overrides por módulo (FROM_TAG). Quando
   * informado, o {@code fromTag} sobrescreve o valor padrão de
   * {@link EntregaModulo#getVersaoFrom()} e é persistido no próprio módulo
   * (registro audit-friendly).
   *
   * @param req se null ou vazio, calcula com defaults.
   */
  @Transactional
  public DeltaResumoResponse calcular(UUID entregaId, CalcularDeltaRequest req) {
    Entrega entrega = entregaService.buscar(entregaId);
    if (!entrega.getStatus().editavel()) {
      throw new BusinessException(
          "Entrega em " + entrega.getStatus() + " — delta só pode ser recalculado em RASCUNHO.");
    }

    List<EntregaModulo> linhas = entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId);
    if (linhas.isEmpty()) {
      throw new BusinessException(
          "Seleção de módulos vazia. Inicialize os módulos antes de calcular o delta.");
    }

    Map<UUID, String> fromOverridePorModulo = new HashMap<>();
    if (req != null && req.modulos() != null) {
      for (var ov : req.modulos()) {
        if (ov.fromTag() != null && !ov.fromTag().isBlank()) {
          fromOverridePorModulo.put(ov.moduloProdutoId(), ov.fromTag().trim());
        }
      }
    }

    repository.deleteByEntregaModulo_Entrega_Id(entregaId);

    List<DeltaResumoResponse.ModuloResumo> resumoPorModulo = new ArrayList<>();
    int totalArtefatos = 0;
    long totalBytes = 0;

    for (EntregaModulo em : linhas) {
      if (!em.isSelecionado()) continue;

      // Aplica override de FROM se houver — persistido para auditoria.
      String overrideFrom = fromOverridePorModulo.get(em.getModuloProduto().getId());
      if (overrideFrom != null) {
        em.setVersaoFrom(overrideFrom);
      }

      List<ArtefatoReleaseModulo> artefatos = artefatoRepository
          .findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(
              entrega.getRelease().getId(), em.getModuloProduto().getId());

      // F2.1/F2.10: sem artefato uploadado, despacha pelo tipo de módulo:
      //   WEB/BATCH → GithubAssetSyncService (download de asset da release)
      //   BANCO     → GithubDeltaBancoService (delta de .sql entre tags)
      // Falha silenciosa: delta fica vazio + log; operador decide se aborta.
      if (artefatos.isEmpty()) {
        try {
          var modulo = em.getModuloProduto();
          artefatos = switch (modulo.getTipo()) {
            case WEB, BATCH -> githubAssetSyncService.sincronizar(
                entrega.getRelease(), modulo);
            case BANCO -> githubDeltaBancoService.sincronizar(
                entrega.getRelease(), modulo, em.getVersaoFrom());
            case KETTLE -> githubDeltaKettleService.sincronizar(
                entrega.getRelease(), modulo, em.getVersaoFrom());
            default -> artefatos;
          };
        } catch (GitHubException ex) {
          log.warn("Falha ao sincronizar GitHub para módulo {}: {}",
              em.getModuloProduto().getCodigo(), ex.getMessage());
        }
      }

      int ordem = 0;
      long bytesDoModulo = 0;
      List<EntregaModuloArtefato> novos = new ArrayList<>();
      for (ArtefatoReleaseModulo art : artefatos) {
        novos.add(new EntregaModuloArtefato(em, art, ordem++));
        bytesDoModulo += art.getTamanhoBytes();
      }
      if (!novos.isEmpty()) {
        repository.saveAll(novos);
      }

      resumoPorModulo.add(new DeltaResumoResponse.ModuloResumo(
          em.getModuloProduto().getId(),
          em.getModuloProduto().getCodigo(),
          em.getModuloProduto().getNome(),
          em.getModuloProduto().getTipo(),
          em.getVersaoFrom(),
          em.getVersaoTo(),
          novos.size(),
          bytesDoModulo));
      totalArtefatos += novos.size();
      totalBytes += bytesDoModulo;
    }

    return new DeltaResumoResponse(entregaId, totalArtefatos, totalBytes, resumoPorModulo);
  }

  /** Resumo sem recalcular — agrupa o que está persistido. */
  public DeltaResumoResponse resumo(UUID entregaId) {
    entregaService.buscar(entregaId);
    List<EntregaModuloArtefato> linhas = repository.findByEntrega_Id(entregaId);
    Map<UUID, Acumulador> porModulo = new LinkedHashMap<>();
    long totalBytes = 0;
    for (EntregaModuloArtefato ema : linhas) {
      var em = ema.getEntregaModulo();
      var modulo = em.getModuloProduto();
      var acc = porModulo.computeIfAbsent(modulo.getId(),
          k -> new Acumulador(em.getVersaoFrom(), em.getVersaoTo(), modulo));
      acc.quantidade++;
      acc.bytes += ema.getArtefato().getTamanhoBytes();
      totalBytes += ema.getArtefato().getTamanhoBytes();
    }
    List<DeltaResumoResponse.ModuloResumo> resumo = porModulo.values().stream()
        .map(a -> new DeltaResumoResponse.ModuloResumo(
            a.modulo.getId(), a.modulo.getCodigo(), a.modulo.getNome(),
            a.modulo.getTipo(), a.from, a.to, a.quantidade, a.bytes))
        .toList();
    return new DeltaResumoResponse(entregaId, linhas.size(), totalBytes, resumo);
  }

  private static final class Acumulador {
    final String from;
    final String to;
    final br.com.softon.portal.releaseorchestrator.entity.ModuloProduto modulo;
    int quantidade;
    long bytes;

    Acumulador(String from, String to,
        br.com.softon.portal.releaseorchestrator.entity.ModuloProduto modulo) {
      this.from = from;
      this.to = to;
      this.modulo = modulo;
    }
  }
}
