package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.BuildInstalacaoArtefato;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildInstalacaoArtefatoRepository extends JpaRepository<BuildInstalacaoArtefato, UUID> {

  @EntityGraph(attributePaths = {"produto"})
  List<BuildInstalacaoArtefato> findTop8ByInstalacao_IdOrderByCreatedAtDesc(UUID instalacaoId);

  @EntityGraph(attributePaths = {"instalacao", "instalacao.host", "produto", "modulo"})
  List<BuildInstalacaoArtefato> findByIdIn(Collection<UUID> ids);
}

