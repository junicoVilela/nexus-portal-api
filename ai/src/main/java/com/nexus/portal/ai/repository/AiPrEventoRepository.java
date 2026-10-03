package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiPrEventoStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPrEventoRepository extends JpaRepository<AiPrEvento, UUID> {

  boolean existsByDeliveryId(String deliveryId);

  Optional<AiPrEvento> findByRepositorioAndNumeroPr(String repositorio, int numeroPr);

  Page<AiPrEvento> findByStatusInOrderByCreatedAtDesc(Collection<AiPrEventoStatus> status, Pageable pageable);

  Page<AiPrEvento> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
