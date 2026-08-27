package com.nexus.portal.releaseorchestrator.repository;

import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProdutoRhRepository extends JpaRepository<ProdutoRh, UUID>,
        JpaSpecificationExecutor<ProdutoRh> {

    boolean existsBySiglaIgnoreCase(String sigla);
    boolean existsBySiglaIgnoreCaseAndIdNot(String sigla, UUID id);

    List<ProdutoRh> findByAtivoTrueOrderBySiglaAsc();
}
