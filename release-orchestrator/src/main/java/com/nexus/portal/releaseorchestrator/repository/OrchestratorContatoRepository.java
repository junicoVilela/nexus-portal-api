package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.Contato;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorContatoRepository extends JpaRepository<Contato, UUID> {

  List<Contato> findByCliente_IdOrderByNomeAsc(UUID clienteId);

  Optional<Contato> findByCliente_IdAndId(UUID clienteId, UUID id);
}
