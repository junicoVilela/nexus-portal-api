package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorFuncionalidadeProdutoRepository
    extends JpaRepository<FuncionalidadeProduto, UUID> {

  List<FuncionalidadeProduto> findByDominio_IdOrderByOrdemAscNomeAsc(UUID dominioId);

  Optional<FuncionalidadeProduto> findByDominio_IdAndId(UUID dominioId, UUID id);

  boolean existsByDominio_IdAndCodigoIgnoreCase(UUID dominioId, String codigo);

  boolean existsByDominio_IdAndCodigoIgnoreCaseAndIdNot(UUID dominioId, String codigo, UUID id);
}
