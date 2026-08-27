package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorInstalacaoClienteRepository
    extends JpaRepository<InstalacaoCliente, UUID>, JpaSpecificationExecutor<InstalacaoCliente> {

  @Override
  @EntityGraph(attributePaths = {"cliente", "host", "produto", "configuracao", "portas"})
  Optional<InstalacaoCliente> findById(UUID id);

  @Override
  @EntityGraph(attributePaths = {"cliente", "host", "produto"})
  Page<InstalacaoCliente> findAll(Specification<InstalacaoCliente> spec, Pageable pageable);

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, UUID id);

  boolean existsByCliente_IdAndProduto_IdAndHost_IdAndAmbiente(
      UUID clienteId, UUID produtoId, UUID hostId, AmbientePadrao ambiente);

  boolean existsByCliente_IdAndProduto_IdAndHost_IdAndAmbienteAndIdNot(
      UUID clienteId, UUID produtoId, UUID hostId, AmbientePadrao ambiente, UUID id);

  boolean existsByHost_Id(UUID hostId);
}
