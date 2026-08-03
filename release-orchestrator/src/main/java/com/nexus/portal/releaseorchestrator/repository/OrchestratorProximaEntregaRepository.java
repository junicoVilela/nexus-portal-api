package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ProximaEntrega;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorProximaEntregaRepository
    extends JpaRepository<ProximaEntrega, UUID>, JpaSpecificationExecutor<ProximaEntrega> {
}
