package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ProtocoloPorta;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorReservaPortaRepository extends JpaRepository<ReservaPorta, UUID> {

  List<ReservaPorta> findByHost_IdAndStatusIn(UUID hostId, Collection<StatusReservaPorta> statuses);

  boolean existsByHost_IdAndPortaAndProtocoloAndStatusIn(
      UUID hostId, int porta, ProtocoloPorta protocolo, Collection<StatusReservaPorta> statuses);

  boolean existsByHost_IdAndPortaAndProtocoloAndStatusInAndInstalacao_IdNot(
      UUID hostId, int porta, ProtocoloPorta protocolo, Collection<StatusReservaPorta> statuses,
      UUID instalacaoId);
}
