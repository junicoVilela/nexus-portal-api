package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorEntregaRepository
    extends JpaRepository<Entrega, UUID>, JpaSpecificationExecutor<Entrega> {
}
