package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ProximaEntrega;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorProximaEntregaRepository
    extends JpaRepository<ProximaEntrega, UUID>, JpaSpecificationExecutor<ProximaEntrega> {
}
