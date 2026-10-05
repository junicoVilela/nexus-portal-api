package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.ManualSinonimo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualSinonimoRepository extends JpaRepository<ManualSinonimo, UUID> {

  List<ManualSinonimo> findByClienteIdOrderByCreatedAtAsc(UUID clienteId);
}
