package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarEntregaRascunhoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.CriarEntregaRequest;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.ProximaEntrega;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.entity.StatusProximaEntrega;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloArtefatoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorProximaEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Agregado central de entrega (F1.8). Cria Entrega em RASCUNHO; reentregas
 * (F1.15) e geração @Async (F1.11) ficam para próximos commits.
 *
 * Conversão de ProximaEntrega: quando o request inclui
 * {@code proximaEntregaId}, a Entrega é criada com os dados dela e a
 * ProximaEntrega vai para CONVERTIDA na mesma transação.
 */
@Service("orchestratorEntregaService")
@RequiredArgsConstructor
public class EntregaService {

  private final OrchestratorEntregaRepository repository;
  private final OrchestratorProximaEntregaRepository proximaEntregaRepository;
  private final OrchestratorEntregaModuloRepository entregaModuloRepository;
  private final OrchestratorEntregaModuloArtefatoRepository deltaRepository;
  private final ClienteService clienteService;
  private final ProdutoRhRepository produtoRepository;
  private final ReleaseRepository releaseRepository;
  private final OrchestratorClienteProdutoRepository clienteProdutoRepository;
  private final EscopoResolver escopoResolver;

  public Entrega buscar(UUID id) {
    Entrega entrega = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Entrega não encontrada."));
    if (!escopoResolver.podeAcessarCliente(entrega.getCliente().getId())) {
      throw new NotFoundException("Entrega não encontrada.");
    }
    return entrega;
  }

  public Page<Entrega> listar(UUID clienteId, UUID produtoId, StatusEntrega status,
      Pageable pageable) {
    Optional<Set<UUID>> permitidos = escopoResolver.clientesPermitidosDoUsuarioAtual();
    if (clienteId != null && permitidos.isPresent() && !permitidos.get().contains(clienteId)) {
      return new PageImpl<>(List.of(), pageable, 0);
    }
    Specification<Entrega> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      if (clienteId != null) preds.add(cb.equal(root.get("cliente").get("id"), clienteId));
      if (produtoId != null) preds.add(cb.equal(root.get("produto").get("id"), produtoId));
      if (status != null) preds.add(cb.equal(root.get("status"), status));
      if (clienteId == null) {
        permitidos.ifPresent(ids -> {
          if (ids.isEmpty()) preds.add(cb.disjunction());
          else preds.add(root.get("cliente").get("id").in(ids));
        });
      }
      return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
    };
    return repository.findAll(spec, pageable);
  }

  /**
   * Cria uma Entrega RASCUNHO. Se {@code proximaEntregaId} for informado,
   * converte a ProximaEntrega (status CONVERTIDA + entregaConvertidaId set).
   */
  @Transactional
  public Entrega criar(CriarEntregaRequest request) {
    request.validar();

    Cliente cliente;
    ProdutoRh produto;
    Release release;
    AmbientePadrao ambiente;
    UUID proximaEntregaId = null;
    ProximaEntrega proximaEntrega = null;

    if (request.veioDeProximaEntrega()) {
      proximaEntrega = proximaEntregaRepository.findById(request.proximaEntregaId())
          .orElseThrow(() -> new NotFoundException("Próxima entrega não encontrada."));
      if (proximaEntrega.getStatus() == StatusProximaEntrega.CONVERTIDA) {
        throw new BusinessException("Próxima entrega já foi convertida em outra entrega.");
      }
      if (proximaEntrega.getStatus() == StatusProximaEntrega.CANCELADA) {
        throw new BusinessException("Próxima entrega cancelada não pode virar Entrega.");
      }
      if (proximaEntrega.getRelease() == null) {
        throw new BusinessException(
            "Próxima entrega sem release definida. Atualize a planejamento antes.");
      }
      cliente = proximaEntrega.getCliente();
      produto = proximaEntrega.getProduto();
      release = proximaEntrega.getRelease();
      ambiente = proximaEntrega.getAmbiente();
      proximaEntregaId = proximaEntrega.getId();
    } else {
      cliente = clienteService.buscar(request.clienteId());
      produto = produtoRepository.findById(request.produtoId())
          .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
      release = resolverRelease(request.releaseId(), produto);
      ambiente = request.ambiente();
      validarContratoCliente(cliente.getId(), produto.getId());
    }

    if (request.entregaOriginalId() != null
        && !repository.existsById(request.entregaOriginalId())) {
      throw new NotFoundException("Entrega original não encontrada para reentrega.");
    }

    escopoResolver.assertPodeEscreverEmCliente(cliente.getId());
    Entrega entrega = new Entrega(cliente, produto, release, ambiente,
        request.responsavelId(), request.observacoes());
    entrega.setProximaEntregaId(proximaEntregaId);
    entrega.setEntregaOriginalId(request.entregaOriginalId());
    entrega = repository.save(entrega);

    if (proximaEntrega != null) {
      proximaEntrega.marcarConvertida(entrega.getId());
    }
    return entrega;
  }

  @Transactional
  public Entrega atualizarRascunho(UUID id, AtualizarEntregaRascunhoRequest request) {
    Entrega entrega = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(entrega.getCliente().getId());
    if (!entrega.getStatus().editavel()) {
      throw new BusinessException(
          "Entrega em " + entrega.getStatus() + " não é editável.");
    }
    entrega.atualizar(request.ambiente(), request.responsavelId(), request.observacoes());
    return entrega;
  }

  /**
   * Cria uma nova Entrega RASCUNHO clonando cliente/produto/release/ambiente
   * + seleção de módulos + delta da entrega original (F1.15).
   *
   * Pré-requisitos: entrega original em estado terminal (CONCLUIDA/FALHA/
   * CANCELADA) — não faz sentido reentregar de algo ainda em rascunho ou
   * gerando. Resultado pronto pra ajustes opcionais antes do /geracao/iniciar.
   */
  @Transactional
  public Entrega reentregar(UUID origemId) {
    Entrega origem = repository.findById(origemId)
        .orElseThrow(() -> new NotFoundException("Entrega original não encontrada."));
    if (!origem.getStatus().terminal()) {
      throw new BusinessException(
          "Reentrega exige entrega original em estado terminal — atual: " + origem.getStatus());
    }

    Entrega nova = new Entrega(origem.getCliente(), origem.getProduto(), origem.getRelease(),
        origem.getAmbiente(), origem.getResponsavelId(), origem.getObservacoes());
    nova.setEntregaOriginalId(origem.getId());
    nova = repository.save(nova);

    // Copia EntregaModulo
    List<EntregaModulo> linhasOrigem = entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(origemId);
    Map<UUID, EntregaModulo> novasPorModulo = new HashMap<>();
    List<EntregaModulo> novasLinhas = new ArrayList<>();
    for (EntregaModulo em : linhasOrigem) {
      EntregaModulo clone = new EntregaModulo(nova, em.getModuloProduto(),
          em.getVersaoFrom(), em.getVersaoTo(),
          em.isSelecionado(), em.isForaContrato(), em.getOrdem());
      novasLinhas.add(clone);
    }
    entregaModuloRepository.saveAll(novasLinhas);
    for (EntregaModulo em : novasLinhas) {
      novasPorModulo.put(em.getModuloProduto().getId(), em);
    }

    // Copia delta (EntregaModuloArtefato) apontando pra novas linhas
    List<EntregaModuloArtefato> deltaOrigem = deltaRepository.findByEntrega_Id(origemId);
    List<EntregaModuloArtefato> deltaNovo = new ArrayList<>();
    for (EntregaModuloArtefato ema : deltaOrigem) {
      UUID moduloId = ema.getEntregaModulo().getModuloProduto().getId();
      EntregaModulo novoEm = novasPorModulo.get(moduloId);
      if (novoEm == null) continue;
      deltaNovo.add(new EntregaModuloArtefato(novoEm, ema.getArtefato(), ema.getOrdem()));
    }
    if (!deltaNovo.isEmpty()) {
      deltaRepository.saveAll(deltaNovo);
    }

    return nova;
  }

  @Transactional
  public Entrega cancelar(UUID id) {
    Entrega entrega = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(entrega.getCliente().getId());
    if (entrega.getStatus().terminal()) {
      throw new BusinessException(
          "Entrega " + entrega.getStatus() + " é terminal — não pode ser cancelada.");
    }
    if (!entrega.getStatus().podeTransicionarPara(StatusEntrega.CANCELADA)) {
      throw new BusinessException(
          "Transição inválida: " + entrega.getStatus() + " → CANCELADA");
    }
    entrega.marcarCancelada();
    return entrega;
  }

  private void validarContratoCliente(UUID clienteId, UUID produtoId) {
    if (!clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId)) {
      throw new BusinessException(
          "Cliente não contrata esse produto. Cadastre o contrato antes da entrega.");
    }
  }

  private Release resolverRelease(UUID releaseId, ProdutoRh produto) {
    Release release = releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    if (!release.getProduto().getId().equals(produto.getId())) {
      throw new BusinessException("Release informada não pertence ao produto da entrega.");
    }
    return release;
  }
}
