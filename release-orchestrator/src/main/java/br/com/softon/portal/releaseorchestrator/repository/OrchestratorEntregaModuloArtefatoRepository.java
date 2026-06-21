package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrchestratorEntregaModuloArtefatoRepository
    extends JpaRepository<EntregaModuloArtefato, UUID> {

  List<EntregaModuloArtefato> findByEntregaModulo_IdOrderByOrdemAsc(UUID entregaModuloId);

  @Query("""
      SELECT ema FROM OrchestratorEntregaModuloArtefato ema
      WHERE ema.entregaModulo.entrega.id = :entregaId
      ORDER BY ema.entregaModulo.ordem ASC, ema.ordem ASC
      """)
  List<EntregaModuloArtefato> findByEntrega_Id(@Param("entregaId") UUID entregaId);

  void deleteByEntregaModulo_Entrega_Id(UUID entregaId);
}
