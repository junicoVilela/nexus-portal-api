package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.ProximaEntregaRequest;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.ProximaEntrega;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.StatusProximaEntrega;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorProximaEntregaRepository;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Agenda + cadastro de próximas entregas (F1.7).
 *
 * Regras:
 * <ul>
 *   <li>Status inicial = PLANEJADA.</li>
 *   <li>Cliente precisa ter contratado o produto.</li>
 *   <li>Se release informada, ela precisa pertencer ao produto.</li>
 *   <li>Transições de status validadas via {@link StatusProximaEntrega}.</li>
 * </ul>
 */
@Service("orchestratorProximaEntregaService")
@RequiredArgsConstructor
public class ProximaEntregaService {

  private final OrchestratorProximaEntregaRepository repository;
  private final ClienteService clienteService;
  private final ProdutoRhRepository produtoRepository;
  private final ReleaseRepository releaseRepository;
  private final OrchestratorClienteProdutoRepository clienteProdutoRepository;

  public ProximaEntrega buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Próxima entrega não encontrada."));
  }

  public Page<ProximaEntrega> listar(UUID clienteId, UUID produtoId, StatusProximaEntrega status,
      LocalDate dataDe, LocalDate dataAte, Pageable pageable) {
    Specification<ProximaEntrega> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      if (clienteId != null) preds.add(cb.equal(root.get("cliente").get("id"), clienteId));
      if (produtoId != null) preds.add(cb.equal(root.get("produto").get("id"), produtoId));
      if (status != null) preds.add(cb.equal(root.get("status"), status));
      if (dataDe != null) preds.add(cb.greaterThanOrEqualTo(root.get("dataPrevista"), dataDe));
      if (dataAte != null) preds.add(cb.lessThanOrEqualTo(root.get("dataPrevista"), dataAte));
      return cb.and(preds.toArray(new Predicate[0]));
    };
    return repository.findAll(spec, pageable);
  }

  @Transactional
  public ProximaEntrega criar(ProximaEntregaRequest request) {
    Cliente cliente = clienteService.buscar(request.clienteId());
    ProdutoRh produto = produtoRepository.findById(request.produtoId())
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
    validarContratoCliente(request.clienteId(), request.produtoId());

    Release release = resolverRelease(request.releaseId(), produto);

    ProximaEntrega entity = new ProximaEntrega(
        cliente, produto, release, request.dataPrevista(),
        request.ambiente(), request.prioridade(),
        request.responsavelId(), request.observacoes());
    return repository.save(entity);
  }

  @Transactional
  public ProximaEntrega atualizar(UUID id, ProximaEntregaRequest request) {
    ProximaEntrega entity = buscar(id);
    if (entity.getStatus().terminal()) {
      throw new BusinessException(
          "Próxima entrega " + entity.getStatus() + " é terminal — não pode ser editada.");
    }
    if (!entity.getCliente().getId().equals(request.clienteId())
        || !entity.getProduto().getId().equals(request.produtoId())) {
      throw new BusinessException("Cliente e produto não podem ser alterados.");
    }
    Release release = resolverRelease(request.releaseId(), entity.getProduto());
    entity.atualizar(release, request.dataPrevista(), request.ambiente(),
        request.prioridade(), request.responsavelId(), request.observacoes());
    return entity;
  }

  @Transactional
  public ProximaEntrega alterarStatus(UUID id, StatusProximaEntrega novo) {
    ProximaEntrega entity = buscar(id);
    if (novo == StatusProximaEntrega.CONVERTIDA) {
      throw new BusinessException(
          "Status CONVERTIDA só pode ser definido ao gerar a Entrega (F1.8).");
    }
    if (!entity.getStatus().podeTransicionarPara(novo)) {
      throw new BusinessException(
          "Transição inválida: " + entity.getStatus() + " → " + novo);
    }
    entity.alterarStatus(novo);
    return entity;
  }

  @Transactional
  public void excluir(UUID id) {
    ProximaEntrega entity = buscar(id);
    if (entity.getStatus() == StatusProximaEntrega.CONVERTIDA) {
      throw new BusinessException(
          "Não é possível excluir uma próxima entrega já convertida em Entrega.");
    }
    repository.delete(entity);
  }

  private void validarContratoCliente(UUID clienteId, UUID produtoId) {
    if (!clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId)) {
      throw new BusinessException(
          "Cliente não contrata esse produto. Cadastre o contrato antes de planejar a entrega.");
    }
  }

  private Release resolverRelease(UUID releaseId, ProdutoRh produto) {
    if (releaseId == null) return null;
    Release release = releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    if (!release.getProduto().getId().equals(produto.getId())) {
      throw new BusinessException("Release informada não pertence ao produto da entrega.");
    }
    return release;
  }
}
