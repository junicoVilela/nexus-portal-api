package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.dto.request.ProjetoRequest;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.repository.ModuloRepository;
import br.com.softon.portal.docflow.repository.ProjetoRepository;
import br.com.softon.rbac.service.AuditoriaService;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.portal.shared.util.SlugUtils;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjetoService {

  private final ProjetoRepository projetoRepository;
  private final ModuloRepository moduloRepository;
  private final AuditoriaService auditoriaService;

  @Transactional
  public Projeto criar(ProjetoRequest request) {
    String slug = slugFrom(request.slug(), request.nome());
    if (projetoRepository.existsBySlug(slug)) {
      throw new BusinessException("Já existe projeto com o slug informado.");
    }
    return projetoRepository.save(new Projeto(request.nome().trim(), slug, request.descricao(),
        request.ativo() == null || request.ativo()));
  }

  @Transactional
  public Projeto atualizar(UUID id, ProjetoRequest request) {
    Projeto projeto = buscar(id);
    String slug = slugFrom(request.slug(), request.nome());
    if (projetoRepository.existsBySlugAndIdNot(slug, id)) {
      throw new BusinessException("Já existe projeto com o slug informado.");
    }
    projeto.atualizar(request.nome().trim(), slug, request.descricao(),
        request.ativo() == null || request.ativo());
    return projeto;
  }

  public Page<Projeto> listar(String nome, Pageable pageable) {
    String filtro = lowerBlankToNull(nome);
    Specification<Projeto> specification = (root, query, criteriaBuilder) -> {
      if (filtro == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.or(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), "%" + filtro + "%"),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), "%" + filtro + "%"));
    };
    return projetoRepository.findAll(specification, pageable);
  }

  public List<Projeto> listar() {
    return projetoRepository.findAllByOrderByNomeAsc();
  }

  public Projeto buscar(UUID id) {
    return projetoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    Projeto projeto = buscar(id);
    if (moduloRepository.existsByProjeto_Id(id)) {
      throw new BusinessException("Exclua primeiro os módulos deste projeto.");
    }
    projetoRepository.delete(projeto);
    auditoriaService.registrar("PROJETO", id, "EXCLUIR", "Projeto excluído: " + projeto.getNome(), principal);
  }

  public List<Projeto> buscarTodos(List<UUID> ids) {
    List<Projeto> projetos = projetoRepository.findAllById(ids);
    if (projetos.size() != ids.stream().distinct().count()) {
      throw new BusinessException("Um ou mais projetos informados não existem.");
    }
    return projetos;
  }

  private String slugFrom(String slug, String nome) {
    String normalized = SlugUtils.normalize(slug == null || slug.isBlank() ? nome : slug);
    if (normalized == null) {
      throw new BusinessException("Slug inválido.");
    }
    return normalized;
  }

  private String lowerBlankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase();
  }
}
