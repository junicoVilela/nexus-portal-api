package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorConfigEntregaRepository extends JpaRepository<ConfigEntrega, UUID> {

  Optional<ConfigEntrega> findByCliente_Id(UUID clienteId);
}
