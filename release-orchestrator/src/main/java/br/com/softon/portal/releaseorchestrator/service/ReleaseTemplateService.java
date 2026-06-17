package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusTemplateRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ReleaseTemplateRequest;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseTemplate;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseTemplateRepository;
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
public class ReleaseTemplateService {

  private final ReleaseTemplateRepository repository;

  @Transactional
  public ReleaseTemplate criar(ReleaseTemplateRequest request) {
    if (repository.existsByNomeIgnoreCase(request.nome().trim())) {
      throw new BusinessException("Já existe um template com o nome informado.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    return repository.save(new ReleaseTemplate(
        request.nome().trim(), request.descricao(), request.tipoRelease(),
        request.produtoId(), request.estrutura(), ativo));
  }

  @Transactional
  public ReleaseTemplate atualizar(UUID id, ReleaseTemplateRequest request) {
    ReleaseTemplate template = buscar(id);
    if (repository.existsByNomeIgnoreCaseAndIdNot(request.nome().trim(), id)) {
      throw new BusinessException("Já existe um template com o nome informado.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    template.atualizar(request.nome().trim(), request.descricao(), request.tipoRelease(),
        request.produtoId(), request.estrutura(), ativo);
    return template;
  }

  @Transactional
  public ReleaseTemplate alterarStatus(UUID id, AlterarStatusTemplateRequest request) {
    ReleaseTemplate template = buscar(id);
    template.alterarStatus(request.ativo());
    return template;
  }

  public Page<ReleaseTemplate> listar(String nome, Boolean ativo, Pageable pageable) {
    Specification<ReleaseTemplate> spec = (root, q, cb) -> cb.conjunction();
    if (nome != null && !nome.isBlank()) {
      String filtro = "%" + nome.toLowerCase().trim() + "%";
      spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("nome")), filtro));
    }
    if (ativo != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
    }
    return repository.findAll(spec, pageable);
  }

  public ReleaseTemplate buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Template não encontrado."));
  }

  @Transactional
  public void excluir(UUID id) {
    ReleaseTemplate template = buscar(id);
    repository.delete(template);
  }
}
