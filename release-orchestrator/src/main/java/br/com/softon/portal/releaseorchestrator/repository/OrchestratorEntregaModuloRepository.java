package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorEntregaModuloRepository extends JpaRepository<EntregaModulo, UUID> {

  List<EntregaModulo> findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(UUID entregaId);

  Optional<EntregaModulo> findByEntrega_IdAndModuloProduto_Id(UUID entregaId, UUID moduloId);

  void deleteByEntrega_Id(UUID entregaId);
}
