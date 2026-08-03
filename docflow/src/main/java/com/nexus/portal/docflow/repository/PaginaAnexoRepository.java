package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.PaginaAnexo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaginaAnexoRepository extends JpaRepository<PaginaAnexo, UUID> {
  @Override
  @EntityGraph(attributePaths = "pagina")
  Optional<PaginaAnexo> findById(UUID id);

  @EntityGraph(attributePaths = "pagina")
  List<PaginaAnexo> findByPagina_IdOrderByCreatedAtDesc(UUID paginaId);

  List<PaginaAnexo> findByPagina_Id(UUID paginaId);

  @Override
  @EntityGraph(attributePaths = {"pagina", "pagina.modulo", "pagina.modulo.projeto"})
  Page<PaginaAnexo> findAll(Pageable pageable);

  @EntityGraph(attributePaths = {"pagina", "pagina.modulo", "pagina.modulo.projeto"})
  Page<PaginaAnexo> findByNomeOriginalContainingIgnoreCase(String nome, Pageable pageable);
}
