package br.com.softon.rbac.service;

import br.com.softon.rbac.entity.AcessoTemporario;
import br.com.softon.rbac.repository.AcessoTemporarioRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
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
public class AcessoTemporarioService {

  private final AcessoTemporarioRepository repository;
  private final AuditoriaService auditoriaService;

  @Transactional
  public AcessoTemporario criar(UUID usuarioId, UUID grupoId, UUID permissaoId, UUID escopoId,
      OffsetDateTime inicioEm, OffsetDateTime fimEm, String justificativa, Principal principal) {
    if (grupoId == null && permissaoId == null && escopoId == null) {
      throw new BusinessException("Informe grupo, permissão ou escopo.");
    }
    if (fimEm == null || inicioEm == null || !fimEm.isAfter(inicioEm)) {
      throw new BusinessException("Janela inválida: fim deve ser posterior ao início.");
    }
    AcessoTemporario a = repository.save(new AcessoTemporario(
        usuarioId, grupoId, permissaoId, escopoId,
        inicioEm, fimEm, justificativa,
        principal == null ? "system" : principal.getName()));
    auditoriaService.registrar("AcessoTemporario", a.getId(), "CRIAR",
        "Vínculo temporário criado para usuário " + usuarioId, principal);
    return a;
  }

  @Transactional
  public AcessoTemporario revogar(UUID id, String motivo, Principal principal) {
    AcessoTemporario a = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Acesso temporário não encontrado."));
    if (a.getRevogadoEm() != null) {
      throw new BusinessException("Acesso temporário já revogado.");
    }
    a.revogar(motivo == null || motivo.isBlank() ? "Revogado manualmente." : motivo);
    auditoriaService.registrar("AcessoTemporario", id, "REVOGAR",
        "Vínculo temporário revogado.", principal);
    return a;
  }

  public Page<AcessoTemporario> listar(AcessoTemporarioFilter filter, Pageable pageable) {
    Specification<AcessoTemporario> spec = (root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      if (filter.usuarioId() != null) ps.add(cb.equal(root.get("usuarioId"), filter.usuarioId()));
      if (filter.grupoId() != null) ps.add(cb.equal(root.get("grupoId"), filter.grupoId()));
      return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(Predicate[]::new));
    };
    return repository.findAll(spec, pageable);
  }

  /** IDs de grupos que o usuário tem por vínculos temporários ativos agora. */
  public List<UUID> gruposAtivosDoUsuario(UUID usuarioId) {
    return repository.findAtivosDoUsuario(usuarioId, OffsetDateTime.now()).stream()
        .map(AcessoTemporario::getGrupoId)
        .filter(id -> id != null)
        .distinct()
        .toList();
  }

  public record AcessoTemporarioFilter(UUID usuarioId, UUID grupoId) {}
}
