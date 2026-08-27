package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.EntregaInstalacao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorEntregaInstalacaoRepository extends JpaRepository<EntregaInstalacao, UUID> {

  boolean existsByInstalacao_Id(UUID instalacaoId);
}
