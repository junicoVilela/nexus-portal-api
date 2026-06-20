package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorClienteProdutoModuloRepository
    extends JpaRepository<ClienteProdutoModulo, UUID> {

  List<ClienteProdutoModulo> findByClienteProduto_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(
      UUID clienteProdutoId);

  Optional<ClienteProdutoModulo> findByClienteProduto_IdAndModuloProduto_Id(
      UUID clienteProdutoId, UUID moduloProdutoId);
}
