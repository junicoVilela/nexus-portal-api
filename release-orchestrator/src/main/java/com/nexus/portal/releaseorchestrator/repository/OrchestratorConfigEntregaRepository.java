package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorConfigEntregaRepository extends JpaRepository<ConfigEntrega, UUID> {

  Optional<ConfigEntrega> findByCliente_Id(UUID clienteId);
}
