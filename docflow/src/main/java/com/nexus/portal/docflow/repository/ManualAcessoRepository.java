package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.ManualAcesso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualAcessoRepository extends JpaRepository<ManualAcesso, UUID> {

  Optional<ManualAcesso> findByTokenHash(String tokenHash);

  List<ManualAcesso> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

  List<ManualAcesso> findByAtivoTrue();
}
