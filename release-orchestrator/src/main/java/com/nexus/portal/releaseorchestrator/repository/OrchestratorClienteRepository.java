package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.Cliente;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorClienteRepository extends JpaRepository<Cliente, UUID>,
    JpaSpecificationExecutor<Cliente> {

  boolean existsBySiglaIgnoreCase(String sigla);

  boolean existsBySiglaIgnoreCaseAndIdNot(String sigla, UUID id);

  boolean existsByCnpj(String cnpj);

  boolean existsByCnpjAndIdNot(String cnpj, UUID id);
}
