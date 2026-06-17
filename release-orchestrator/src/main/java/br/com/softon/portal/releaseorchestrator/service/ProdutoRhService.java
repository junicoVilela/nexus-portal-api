package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ProdutoRhRequest;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProdutoRhService {

  private final ProdutoRhRepository repository;
  private final ReleaseRepository releaseRepository;

  @Transactional
  public ProdutoRh criar(ProdutoRhRequest request) {
    String sigla = request.sigla().toUpperCase().trim();
    if (repository.existsBySiglaIgnoreCase(sigla)) {
      throw new BusinessException("Já existe um produto com a sigla informada.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    return repository.save(new ProdutoRh(
        request.nome().trim(), sigla, request.descricao(),
        request.cor(), request.responsavelId(), ativo));
  }

  @Transactional
  public ProdutoRh atualizar(UUID id, ProdutoRhRequest request) {
    ProdutoRh produto = buscar(id);
    String sigla = request.sigla().toUpperCase().trim();
    if (repository.existsBySiglaIgnoreCaseAndIdNot(sigla, id)) {
      throw new BusinessException("Já existe um produto com a sigla informada.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    produto.atualizar(request.nome().trim(), sigla, request.descricao(),
        request.cor(), request.responsavelId(), ativo);
    return produto;
  }

  @Transactional
  public ProdutoRh alterarStatus(UUID id, AlterarStatusProdutoRequest request) {
    ProdutoRh produto = buscar(id);
    produto.alterarStatus(request.ativo());
    return produto;
  }

  public Page<ProdutoRh> listar(String nome, Boolean ativo, Pageable pageable) {
    Specification<ProdutoRh> spec = (root, q, cb) -> cb.conjunction();
    if (nome != null && !nome.isBlank()) {
      String filtro = "%" + nome.toLowerCase().trim() + "%";
      spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("nome")), filtro));
    }
    if (ativo != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
    }
    return repository.findAll(spec, pageable);
  }

  public ProdutoRh buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
  }

  @Transactional
  public void excluir(UUID id) {
    ProdutoRh produto = buscar(id);
    if (releaseRepository.countByProdutoId(id) > 0) {
      throw new BusinessException("Não é possível excluir produto com releases vinculadas.");
    }
    repository.delete(produto);
  }
}
