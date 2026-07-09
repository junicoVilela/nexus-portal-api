package br.com.softon.rbac.service;

import br.com.softon.rbac.entity.HistoricoLogin;
import br.com.softon.rbac.repository.HistoricoLoginRepository;
import jakarta.persistence.criteria.Predicate;
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
public class HistoricoLoginService {

  private final HistoricoLoginRepository repository;

  public void registrar(UUID usuarioId, String login, String ipOrigem, String userAgent,
      boolean sucesso, String motivoFalha) {
    repository.save(new HistoricoLogin(usuarioId, login, ipOrigem, userAgent, sucesso, motivoFalha));
  }

  public Page<HistoricoLogin> listar(HistoricoLoginFilter filter, Pageable pageable) {
    Specification<HistoricoLogin> spec = (root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      if (filter.usuarioId() != null) {
        ps.add(cb.equal(root.get("usuarioId"), filter.usuarioId()));
      }
      if (filter.login() != null && !filter.login().isBlank()) {
        ps.add(cb.like(cb.lower(root.get("loginInformado")),
            "%" + filter.login().toLowerCase() + "%"));
      }
      if (filter.sucesso() != null) {
        ps.add(cb.equal(root.get("sucesso"), filter.sucesso()));
      }
      if (filter.inicio() != null) {
        ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.inicio()));
      }
      if (filter.fim() != null) {
        ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.fim()));
      }
      return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(Predicate[]::new));
    };
    return repository.findAll(spec, pageable);
  }

  public record HistoricoLoginFilter(
      UUID usuarioId,
      String login,
      Boolean sucesso,
      OffsetDateTime inicio,
      OffsetDateTime fim) {
  }
}
