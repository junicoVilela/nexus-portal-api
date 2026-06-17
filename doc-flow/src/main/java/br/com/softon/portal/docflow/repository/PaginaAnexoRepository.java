package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.PaginaAnexo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaginaAnexoRepository extends JpaRepository<PaginaAnexo, UUID> {
  @Override
  @EntityGraph(attributePaths = "pagina")
  Optional<PaginaAnexo> findById(UUID id);

  @EntityGraph(attributePaths = "pagina")
  List<PaginaAnexo> findByPagina_IdOrderByCreatedAtDesc(UUID paginaId);

  List<PaginaAnexo> findByPagina_Id(UUID paginaId);
}
