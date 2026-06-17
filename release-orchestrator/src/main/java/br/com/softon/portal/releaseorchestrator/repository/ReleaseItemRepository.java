package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.ReleaseItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReleaseItemRepository extends JpaRepository<ReleaseItem, UUID> {

    List<ReleaseItem> findByReleaseIdOrderByOrdemAsc(UUID releaseId);

    long countByReleaseId(UUID releaseId);

    @Query("SELECT COALESCE(MAX(i.ordem), 0) FROM ReleaseItem i WHERE i.release.id = :releaseId")
    int findMaxOrdemByReleaseId(@Param("releaseId") UUID releaseId);
}
