package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ReleaseHistorico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseHistoricoRepository extends JpaRepository<ReleaseHistorico, UUID> {

    List<ReleaseHistorico> findByReleaseIdOrderByCreatedAtDesc(UUID releaseId);
}
