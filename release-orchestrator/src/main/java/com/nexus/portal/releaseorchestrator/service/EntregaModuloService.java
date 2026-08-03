package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.ClienteProduto;
import com.nexus.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseModuloVersaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
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
 * Seleção de módulos da entrega (F1.9).
 *
 * Inicialização cruza:
 * <ul>
 *   <li>Módulos contratados pelo cliente (ClienteProdutoModulo) — versão atual.</li>
 *   <li>Módulos com versão na release (ReleaseModuloVersao) — versão alvo.</li>
 * </ul>
 *
 * Auto-marca selecionado=true quando o módulo contratado mudou de versão
 * (versaoFrom != versaoTo). Módulos da release que não estão no contrato
 * entram com {@code foraContrato=true} e selecionado=false — operador
 * decide se inclui.
 *
 * Só permite operação enquanto a entrega está em RASCUNHO.
 */
@Service("orchestratorEntregaModuloService")
@RequiredArgsConstructor
public class EntregaModuloService {

  private final OrchestratorEntregaModuloRepository repository;
  private final EntregaService entregaService;
  private final OrchestratorClienteProdutoRepository clienteProdutoRepository;
  private final OrchestratorClienteProdutoModuloRepository clienteProdutoModuloRepository;
  private final ReleaseModuloVersaoRepository releaseModuloVersaoRepository;

  public List<EntregaModulo> listar(UUID entregaId) {
    entregaService.buscar(entregaId);
    return repository.findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId);
  }

  /**
   * Limpa e reconstrói a seleção a partir do contrato + release.
   * Idempotente: pode ser chamado várias vezes em RASCUNHO.
   */
  @Transactional
  public List<EntregaModulo> inicializar(UUID entregaId) {
    Entrega entrega = entregaService.buscar(entregaId);
    if (!entrega.getStatus().editavel()) {
      throw new BusinessException(
          "Entrega em " + entrega.getStatus()
              + " — seleção de módulos só pode ser inicializada em RASCUNHO.");
    }

    ClienteProduto contrato = clienteProdutoRepository
        .findByCliente_IdAndProduto_Id(entrega.getCliente().getId(), entrega.getProduto().getId())
        .orElseThrow(() -> new NotFoundException(
            "Contrato cliente×produto não encontrado para esta entrega."));

    // Versão atual por módulo (do contrato)
    List<ClienteProdutoModulo> contratados = clienteProdutoModuloRepository
        .findByClienteProduto_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(
            contrato.getId());
    Map<UUID, String> versaoAtualPorModulo = new HashMap<>();
    Map<UUID, ModuloProduto> contratadoPorModulo = new LinkedHashMap<>();
    for (ClienteProdutoModulo cpm : contratados) {
      if (!cpm.isAtivo()) continue;
      versaoAtualPorModulo.put(cpm.getModuloProduto().getId(), cpm.getVersaoAtual());
      contratadoPorModulo.put(cpm.getModuloProduto().getId(), cpm.getModuloProduto());
    }

    // Versão alvo por módulo (da release)
    List<ReleaseModuloVersao> daRelease = releaseModuloVersaoRepository
        .findByRelease_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(
            entrega.getRelease().getId());
    Map<UUID, String> versaoTargetPorModulo = new HashMap<>();
    Map<UUID, ModuloProduto> daReleasePorModulo = new LinkedHashMap<>();
    for (ReleaseModuloVersao rmv : daRelease) {
      versaoTargetPorModulo.put(rmv.getModuloProduto().getId(), rmv.getVersao());
      daReleasePorModulo.put(rmv.getModuloProduto().getId(), rmv.getModuloProduto());
    }

    // Limpa seleção anterior
    repository.deleteByEntrega_Id(entregaId);

    // Monta linhas: primeiro contratados (ordem preservada), depois release-only
    List<EntregaModulo> novas = new ArrayList<>();
    int ordem = 0;
    for (Map.Entry<UUID, ModuloProduto> entry : contratadoPorModulo.entrySet()) {
      ModuloProduto m = entry.getValue();
      String from = versaoAtualPorModulo.get(m.getId());
      String to = versaoTargetPorModulo.get(m.getId());
      boolean mudou = to != null && (from == null || !from.equalsIgnoreCase(to));
      novas.add(new EntregaModulo(entrega, m, from, to, mudou, false, ordem++));
    }
    for (Map.Entry<UUID, ModuloProduto> entry : daReleasePorModulo.entrySet()) {
      if (contratadoPorModulo.containsKey(entry.getKey())) continue;
      ModuloProduto m = entry.getValue();
      String to = versaoTargetPorModulo.get(m.getId());
      novas.add(new EntregaModulo(entrega, m, null, to, false, true, ordem++));
    }

    return repository.saveAll(novas);
  }

  @Transactional
  public EntregaModulo alterarSelecao(UUID entregaId, UUID moduloProdutoId,
      boolean selecionado) {
    Entrega entrega = entregaService.buscar(entregaId);
    if (!entrega.getStatus().editavel()) {
      throw new BusinessException(
          "Entrega em " + entrega.getStatus() + " — seleção é imutável.");
    }
    EntregaModulo em = repository
        .findByEntrega_IdAndModuloProduto_Id(entregaId, moduloProdutoId)
        .orElseThrow(() -> new NotFoundException(
            "Linha de seleção não encontrada. Inicialize a seleção primeiro."));
    em.alterarSelecao(selecionado);
    return em;
  }
}
