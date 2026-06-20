package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorClienteProdutoRepository extends JpaRepository<ClienteProduto, UUID> {

  List<ClienteProduto> findByCliente_IdOrderByProduto_NomeAsc(UUID clienteId);

  Optional<ClienteProduto> findByCliente_IdAndId(UUID clienteId, UUID id);

  Optional<ClienteProduto> findByCliente_IdAndProduto_Id(UUID clienteId, UUID produtoId);

  boolean existsByCliente_IdAndProduto_Id(UUID clienteId, UUID produtoId);
}
