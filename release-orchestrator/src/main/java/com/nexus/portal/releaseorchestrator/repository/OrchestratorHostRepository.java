package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.Host;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorHostRepository extends JpaRepository<Host, UUID>,
    JpaSpecificationExecutor<Host> {

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, UUID id);

  boolean existsByHostnameIgnoreCase(String hostname);

  boolean existsByHostnameIgnoreCaseAndIdNot(String hostname, UUID id);
}
