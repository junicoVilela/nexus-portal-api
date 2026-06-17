package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.ClienteProjeto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteProjetoRepository extends JpaRepository<ClienteProjeto, UUID> {
  void deleteByCliente_Id(UUID clienteId);

  @Query("select cp.projeto.id from ClienteProjeto cp where cp.cliente.id = :clienteId")
  List<UUID> findProjetoIdsByClienteId(@Param("clienteId") UUID clienteId);
}
