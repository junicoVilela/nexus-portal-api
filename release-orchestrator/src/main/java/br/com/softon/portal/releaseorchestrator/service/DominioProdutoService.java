package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.DominioProdutoRequest;
import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorDominioProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CRUD do catálogo de domínios funcionais por produto (F1.3 — Camada A).
 *
 * Regras:
 * <ul>
 *   <li>`codigo` único por produto, regex {@code [a-z0-9_-]+}.</li>
 *   <li>`codigo` é imutável após criação (mesma regra da F0.3).</li>
 *   <li>Exclusão hard cascateia funcionalidades (DB ON DELETE CASCADE).</li>
 * </ul>
 */
@Service("orchestratorDominioProdutoService")
@RequiredArgsConstructor
public class DominioProdutoService {

  private final OrchestratorDominioProdutoRepository repository;
  private final ProdutoRhRepository produtoRepository;

  public List<DominioProduto> listar(UUID produtoId) {
    requerirProduto(produtoId);
    return repository.findByProduto_IdOrderByOrdemAscNomeAsc(produtoId);
  }

  public DominioProduto buscar(UUID produtoId, UUID id) {
    return repository.findByProduto_IdAndId(produtoId, id)
        .orElseThrow(() -> new NotFoundException("Domínio não encontrado para o produto."));
  }

  @Transactional
  public DominioProduto criar(UUID produtoId, DominioProdutoRequest request) {
    ProdutoRh produto = requerirProduto(produtoId);
    if (repository.existsByProduto_IdAndCodigoIgnoreCase(produtoId, request.codigo())) {
      throw new BusinessException("Já existe um domínio com esse código neste produto.");
    }
    int ordem = request.ordem() != null ? request.ordem() : 0;
    return repository.save(new DominioProduto(produto, request.nome(), request.codigo(),
        request.codigoLegado(), request.descricao(), ordem));
  }

  /** Não altera o {@code codigo} (imutável). */
  @Transactional
  public DominioProduto atualizar(UUID produtoId, UUID id, DominioProdutoRequest request) {
    DominioProduto dominio = buscar(produtoId, id);
    int ordem = request.ordem() != null ? request.ordem() : dominio.getOrdem();
    dominio.atualizar(request.nome(), request.codigoLegado(), request.descricao(), ordem);
    return dominio;
  }

  @Transactional
  public DominioProduto alterarStatus(UUID produtoId, UUID id, boolean ativo) {
    DominioProduto dominio = buscar(produtoId, id);
    dominio.alterarStatus(ativo);
    return dominio;
  }

  @Transactional
  public void excluir(UUID produtoId, UUID id) {
    DominioProduto dominio = buscar(produtoId, id);
    repository.delete(dominio);
  }

  private ProdutoRh requerirProduto(UUID produtoId) {
    return produtoRepository.findById(produtoId)
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
  }
}
