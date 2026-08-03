package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.request.ModuloRequest;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.repository.ModuloRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.portal.shared.util.SlugUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModuloService {

  private final ModuloRepository moduloRepository;
  private final ProjetoService projetoService;
  private final PaginaRepository paginaRepository;
  private final AuditoriaService auditoriaService;

  @Transactional
  public Modulo criar(ModuloRequest request) {
    String slug = slugFrom(request.slug(), request.nome());
    Projeto projeto = projeto(request.projetoId());
    if (moduloRepository.existsBySlugAndProjeto_Id(slug, projeto.getId())) {
      throw new BusinessException("Já existe módulo com o slug informado.");
    }
    Modulo modulo = new Modulo(request.nome().trim(), slug, request.descricao(),
        request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo(), projeto);
    return moduloRepository.save(modulo);
  }

  @Transactional
  public Modulo atualizar(UUID id, ModuloRequest request) {
    Modulo modulo = buscar(id);
    String slug = slugFrom(request.slug(), request.nome());
    Projeto projeto = projeto(request.projetoId());
    if (moduloRepository.existsBySlugAndProjeto_IdAndIdNot(slug, projeto.getId(), id)) {
      throw new BusinessException("Já existe módulo com o slug informado.");
    }
    modulo.atualizar(request.nome().trim(), slug, request.descricao(),
        request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo(), projeto);
    return modulo;
  }

  public Page<Modulo> listar(UUID projetoId, String nome, Pageable pageable) {
    String filtro = lowerBlankToNull(nome);
    Specification<Modulo> specification = (root, query, criteriaBuilder) -> {
      if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
        root.fetch("projeto");
        query.distinct(true);
      }
      List<Predicate> predicates = new ArrayList<>();
      if (projetoId != null) {
        predicates.add(criteriaBuilder.equal(root.get("projeto").get("id"), projetoId));
      }
      if (filtro != null) {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), "%" + filtro + "%"),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), "%" + filtro + "%")));
      }
      return predicates.isEmpty()
          ? criteriaBuilder.conjunction()
          : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
    return moduloRepository.findAll(specification, pageable);
  }

  public List<Modulo> listar(UUID projetoId) {
    return projetoId == null
        ? moduloRepository.findAllByOrderByProjeto_NomeAscOrdemAscNomeAsc()
        : moduloRepository.findByProjeto_IdOrderByOrdemAscNomeAsc(projetoId);
  }

  public Modulo buscar(UUID id) {
    return moduloRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    Modulo modulo = buscar(id);
    if (paginaRepository.existsByModulo_Id(id)) {
      throw new BusinessException("Exclua primeiro as páginas deste módulo.");
    }
    moduloRepository.delete(modulo);
    auditoriaService.registrar("MODULO", id, "EXCLUIR", "Módulo excluído: " + modulo.getNome(), principal);
  }

  public List<Modulo> buscarTodos(List<UUID> ids) {
    List<Modulo> modulos = moduloRepository.findAllById(ids);
    if (modulos.size() != ids.stream().distinct().count()) {
      throw new BusinessException("Um ou mais módulos informados não existem.");
    }
    return modulos;
  }

  private String slugFrom(String slug, String nome) {
    String normalized = SlugUtils.normalize(slug == null || slug.isBlank() ? nome : slug);
    if (normalized == null) {
      throw new BusinessException("Slug inválido.");
    }
    return normalized;
  }

  private Projeto projeto(UUID id) {
    return projetoService.buscar(id);
  }

  private String lowerBlankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase();
  }
}
