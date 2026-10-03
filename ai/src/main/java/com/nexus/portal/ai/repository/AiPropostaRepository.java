package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPropostaRepository extends JpaRepository<AiProposta, UUID> {

  Optional<AiProposta> findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(
      UUID sessaoId, AiPropostaStatus status);

  Optional<AiProposta> findFirstBySessaoIdOrderByCreatedAtDesc(UUID sessaoId);

  /** Base do painel de qualidade (volume interno: agregação em memória). */
  List<AiProposta> findByCreatedAtAfter(OffsetDateTime desde);
}
