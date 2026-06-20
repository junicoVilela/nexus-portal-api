package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorDominioProdutoRepository extends JpaRepository<DominioProduto, UUID> {

  List<DominioProduto> findByProduto_IdOrderByOrdemAscNomeAsc(UUID produtoId);

  Optional<DominioProduto> findByProduto_IdAndId(UUID produtoId, UUID id);

  boolean existsByProduto_IdAndCodigoIgnoreCase(UUID produtoId, String codigo);

  boolean existsByProduto_IdAndCodigoIgnoreCaseAndIdNot(UUID produtoId, String codigo, UUID id);
}
