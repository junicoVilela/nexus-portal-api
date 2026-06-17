package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.Projeto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID>, JpaSpecificationExecutor<Projeto> {
  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);

  List<Projeto> findAllByOrderByNomeAsc();
}
