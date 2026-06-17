package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReleaseRepository extends JpaRepository<Release, UUID>,
        JpaSpecificationExecutor<Release> {

    @Override
    @EntityGraph(attributePaths = "produto")
    Optional<Release> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "produto")
    Page<Release> findAll(Specification<Release> spec, Pageable pageable);

    boolean existsByProdutoIdAndVersao(UUID produtoId, String versao);
    boolean existsByProdutoIdAndVersaoAndIdNot(UUID produtoId, String versao, UUID id);

    long countByStatus(ReleaseStatus status);

    @Query("SELECT COUNT(r) FROM Release r WHERE r.status NOT IN ('PUBLICADA', 'CANCELADA')")
    long countAtivas();

    @Query("SELECT COUNT(r) FROM Release r WHERE r.produto.id = :produtoId")
    long countByProdutoId(@Param("produtoId") UUID produtoId);
}
