package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.DeployInstalacao;
import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.StatusDeploy;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorDeployInstalacaoRepository
    extends JpaRepository<DeployInstalacao, UUID>, JpaSpecificationExecutor<DeployInstalacao> {

  @Override
  @EntityGraph(attributePaths = {"release", "instalacao", "instalacao.host", "instalacao.cliente", "entrega"})
  Optional<DeployInstalacao> findById(UUID id);

  @Override
  @EntityGraph(attributePaths = {"release", "instalacao", "instalacao.host", "instalacao.cliente", "entrega"})
  Page<DeployInstalacao> findAll(Specification<DeployInstalacao> spec, Pageable pageable);

  Optional<DeployInstalacao> findFirstByRelease_IdAndInstalacao_IdAndFingerprintAndModoAndStatusOrderByCreatedAtDesc(
      UUID releaseId, UUID instalacaoId, String fingerprint, ModoDeploy modo, StatusDeploy status);

  @EntityGraph(attributePaths = {"release", "release.produto"})
  Optional<DeployInstalacao> findFirstByInstalacao_IdAndOperacaoInAndStatusOrderByCreatedAtDesc(
      UUID instalacaoId, Collection<OperacaoDeploy> operacoes, StatusDeploy status);

  boolean existsByInstalacao_Id(UUID instalacaoId);
}
