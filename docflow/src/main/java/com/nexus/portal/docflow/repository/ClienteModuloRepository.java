package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.ClienteModulo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteModuloRepository extends JpaRepository<ClienteModulo, UUID> {
  void deleteByCliente_Id(UUID clienteId);

  List<ClienteModulo> findByCliente_Id(UUID clienteId);

  @Query("select cm.modulo.id from ClienteModulo cm where cm.cliente.id = :clienteId")
  List<UUID> findModuloIdsByClienteId(@Param("clienteId") UUID clienteId);

}
