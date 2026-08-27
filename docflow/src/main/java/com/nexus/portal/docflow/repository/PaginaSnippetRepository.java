package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.PaginaSnippet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaginaSnippetRepository extends JpaRepository<PaginaSnippet, UUID> {

  Optional<PaginaSnippet> findByCodigo(String codigo);

  boolean existsByCodigo(String codigo);

  List<PaginaSnippet> findByAtivoTrueOrderByCodigoAsc();

  List<PaginaSnippet> findAllByOrderByCodigoAsc();
}
