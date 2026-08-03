package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtefatoReleaseModuloRepository extends JpaRepository<ArtefatoReleaseModulo, UUID> {

  List<ArtefatoReleaseModulo> findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(
      UUID releaseId, UUID moduloId);

  Optional<ArtefatoReleaseModulo> findByRelease_IdAndModuloProduto_IdAndId(
      UUID releaseId, UUID moduloId, UUID id);
}
