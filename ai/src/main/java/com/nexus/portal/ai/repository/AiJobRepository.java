package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

  boolean existsBySessaoIdAndStatusIn(UUID sessaoId, Iterable<AiJobStatus> statuses);

  Optional<AiJob> findFirstBySessaoIdOrderByCreatedAtDesc(UUID sessaoId);
}
