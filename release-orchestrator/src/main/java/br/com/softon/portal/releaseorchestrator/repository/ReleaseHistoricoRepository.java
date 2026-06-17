package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ReleaseHistorico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseHistoricoRepository extends JpaRepository<ReleaseHistorico, UUID> {

    List<ReleaseHistorico> findByReleaseIdOrderByCreatedAtDesc(UUID releaseId);
}
