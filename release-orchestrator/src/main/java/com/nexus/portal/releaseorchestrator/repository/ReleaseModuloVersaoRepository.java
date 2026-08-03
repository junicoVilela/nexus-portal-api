package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseModuloVersaoRepository extends JpaRepository<ReleaseModuloVersao, UUID> {

  List<ReleaseModuloVersao> findByRelease_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(
      UUID releaseId);

  Optional<ReleaseModuloVersao> findByRelease_IdAndModuloProduto_Id(UUID releaseId, UUID moduloId);
}
