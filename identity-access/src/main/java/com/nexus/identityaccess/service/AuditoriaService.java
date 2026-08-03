package com.nexus.identityaccess.service;

import com.nexus.identityaccess.entity.AuditoriaEvento;
import com.nexus.identityaccess.repository.AuditoriaRepository;
import jakarta.persistence.criteria.Predicate;
import java.security.Principal;
import java.time.OffsetDateTime;
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
public class AuditoriaService {

  private final AuditoriaRepository auditoriaRepository;

  public void registrar(String entidade, UUID entidadeId, String acao, String descricao, Principal principal) {
    auditoriaRepository.save(new AuditoriaEvento(entidade, entidadeId, acao, descricao, username(principal)));
  }

  public List<AuditoriaEvento> recentes() {
    return auditoriaRepository.findTop100ByOrderByCreatedAtDesc();
  }

  public Page<AuditoriaEvento> listar(Pageable pageable) {
    return listar(new AuditoriaFilter(null, null, null, null, null, null), pageable);
  }

  public Page<AuditoriaEvento> listar(AuditoriaFilter filter, Pageable pageable) {
    Specification<AuditoriaEvento> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.usuario() != null && !filter.usuario().isBlank()) {
        predicates.add(cb.equal(cb.lower(root.get("createdBy")), filter.usuario().toLowerCase()));
      }
      if (filter.acao() != null && !filter.acao().isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("acao")), "%" + filter.acao().toLowerCase() + "%"));
      }
      if (filter.entidade() != null && !filter.entidade().isBlank()) {
        predicates.add(cb.equal(cb.lower(root.get("entidade")), filter.entidade().toLowerCase()));
      }
      if (filter.entidadeId() != null) {
        predicates.add(cb.equal(root.get("entidadeId"), filter.entidadeId()));
      }
      if (filter.inicio() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.inicio()));
      }
      if (filter.fim() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.fim()));
      }
      return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
    };
    return auditoriaRepository.findAll(spec, pageable);
  }

  private String username(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }

  public record AuditoriaFilter(
      String usuario,
      String acao,
      String entidade,
      UUID entidadeId,
      OffsetDateTime inicio,
      OffsetDateTime fim) {
  }
}
