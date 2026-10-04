package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiManualPergunta;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiManualPerguntaRepository extends JpaRepository<AiManualPergunta, UUID> {

  List<AiManualPergunta> findByCreatedAtAfter(OffsetDateTime desde);
}
