package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ClienteFuncionalidadeProduto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrchestratorClienteFuncionalidadeRepository
    extends JpaRepository<ClienteFuncionalidadeProduto, UUID> {

  List<ClienteFuncionalidadeProduto> findByCliente_Id(UUID clienteId);

  Optional<ClienteFuncionalidadeProduto> findByCliente_IdAndFuncionalidade_Id(
      UUID clienteId, UUID funcionalidadeId);

  /** Linhas da matriz para um cliente + produto específico (join via domínio). */
  @Query("""
      SELECT cf FROM OrchestratorClienteFuncionalidadeProduto cf
      WHERE cf.cliente.id = :clienteId
        AND cf.funcionalidade.dominio.produto.id = :produtoId
      """)
  List<ClienteFuncionalidadeProduto> findByClienteEProduto(
      @Param("clienteId") UUID clienteId,
      @Param("produtoId") UUID produtoId);
}
