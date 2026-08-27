package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ManifestoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrchestratorManifestoImplantacaoRepository extends JpaRepository<ManifestoImplantacao, UUID> {

  Optional<ManifestoImplantacao> findByRelease_IdAndTipoImplantacao(UUID releaseId, TipoImplantacao tipo);

  List<ManifestoImplantacao> findByRelease_IdOrderByTipoImplantacaoAsc(UUID releaseId);
}
