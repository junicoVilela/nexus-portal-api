package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloProdutoRepository extends JpaRepository<ModuloProduto, UUID> {

  List<ModuloProduto> findByProduto_IdOrderByOrdemAscNomeAsc(UUID produtoId);

  Optional<ModuloProduto> findByProduto_IdAndId(UUID produtoId, UUID id);

  boolean existsByProduto_IdAndCodigoIgnoreCase(UUID produtoId, String codigo);

  boolean existsByProduto_IdAndCodigoIgnoreCaseAndIdNot(UUID produtoId, String codigo, UUID id);
}
