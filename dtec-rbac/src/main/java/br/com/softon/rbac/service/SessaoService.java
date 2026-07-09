package br.com.softon.rbac.service;

import br.com.softon.rbac.entity.Sessao;
import br.com.softon.rbac.repository.SessaoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.portal.shared.security.SessaoValidator;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessaoService implements SessaoValidator {

  private final SessaoRepository repository;
  private final AuditoriaService auditoriaService;

  @Transactional
  public Sessao abrir(String jti, UUID usuarioId, String ipOrigem, String userAgent,
      OffsetDateTime expiraEm) {
    return repository.save(new Sessao(jti, usuarioId, ipOrigem, userAgent, expiraEm));
  }

  /**
   * True se o jti existe, está ativo e não expirou. Fail-open quando o jti
   * é nulo ou vazio (token legado sem jti) — evita quebrar sessões
   * emitidas antes do rollout.
   */
  @Override
  public boolean sessaoAtiva(String jti) {
    if (jti == null || jti.isBlank()) return true;
    return repository.findByJti(jti)
        .filter(Sessao::isAtiva)
        .filter(s -> s.getExpiraEm() == null || s.getExpiraEm().isAfter(OffsetDateTime.now()))
        .isPresent();
  }

  public Page<Sessao> listar(SessaoFilter filter, Pageable pageable) {
    Specification<Sessao> spec = (root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      if (filter.usuarioId() != null) ps.add(cb.equal(root.get("usuarioId"), filter.usuarioId()));
      if (filter.ativa() != null) ps.add(cb.equal(root.get("ativa"), filter.ativa()));
      return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(Predicate[]::new));
    };
    return repository.findAll(spec, pageable);
  }

  @Transactional
  public Sessao revogar(UUID sessaoId, String motivo, Principal principal) {
    Sessao s = repository.findById(sessaoId)
        .orElseThrow(() -> new NotFoundException("Sessão não encontrada."));
    if (!s.isAtiva()) {
      throw new BusinessException("Sessão já está encerrada.");
    }
    s.revogar(motivo == null || motivo.isBlank() ? "Revogada manualmente." : motivo);
    auditoriaService.registrar("Sessao", sessaoId, "REVOGAR",
        "Sessão revogada: " + s.getMotivoEncerramento(), principal);
    return s;
  }

  @Transactional
  public void encerrarPorLogout(String jti) {
    repository.findByJti(jti).ifPresent(s -> {
      if (s.isAtiva()) s.encerrarPorLogout();
    });
  }

  public record SessaoFilter(UUID usuarioId, Boolean ativa) {}
}
