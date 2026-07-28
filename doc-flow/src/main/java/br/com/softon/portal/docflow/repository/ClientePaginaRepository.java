package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.ClientePagina;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientePaginaRepository extends JpaRepository<ClientePagina, UUID> {
  void deleteByCliente_Id(UUID clienteId);

  List<ClientePagina> findByCliente_Id(UUID clienteId);

  @Query("select cp.pagina.id from ClientePagina cp where cp.cliente.id = :clienteId")
  List<UUID> findPaginaIdsByClienteId(@Param("clienteId") UUID clienteId);

}
