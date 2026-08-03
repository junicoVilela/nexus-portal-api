package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiSessao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSessaoRepository extends JpaRepository<AiSessao, UUID> {
}
