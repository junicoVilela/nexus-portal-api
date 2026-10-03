package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

  Optional<AiJob> findFirstBySessaoIdAndStatusInOrderByCreatedAtDesc(
      UUID sessaoId, Iterable<AiJobStatus> statuses);

  List<AiJob> findAllBySessaoIdAndStatusIn(UUID sessaoId, Iterable<AiJobStatus> statuses);

  Optional<AiJob> findFirstBySessaoIdOrderByCreatedAtDesc(UUID sessaoId);

  long countBySessaoId(UUID sessaoId);

  /** Gerações disparadas pelo usuário (dono da sessão) desde {@code desde} — base do rate limit. */
  long countBySessaoCreatedByAndCreatedAtAfter(String usuario, OffsetDateTime desde);

  List<AiJob> findAllByStatus(AiJobStatus status);

  /** Base do painel de qualidade (volume interno: agregação em memória). */
  List<AiJob> findByCreatedAtAfter(OffsetDateTime desde);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select j from AiJob j join fetch j.sessao where j.id = :id")
  Optional<AiJob> findByIdForUpdate(@Param("id") UUID id);
}
