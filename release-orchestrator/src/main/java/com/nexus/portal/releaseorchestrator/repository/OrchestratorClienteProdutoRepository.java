package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ClienteProduto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorClienteProdutoRepository extends JpaRepository<ClienteProduto, UUID> {

  @EntityGraph(attributePaths = {"produto", "cliente"})
  List<ClienteProduto> findByCliente_IdOrderByProduto_NomeAsc(UUID clienteId);

  Optional<ClienteProduto> findByCliente_IdAndId(UUID clienteId, UUID id);

  Optional<ClienteProduto> findByCliente_IdAndProduto_Id(UUID clienteId, UUID produtoId);

  boolean existsByCliente_IdAndProduto_Id(UUID clienteId, UUID produtoId);
}
