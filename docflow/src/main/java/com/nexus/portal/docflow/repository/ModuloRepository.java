package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.Modulo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ModuloRepository extends JpaRepository<Modulo, UUID>, JpaSpecificationExecutor<Modulo> {
  long countByAtivoTrue();

  boolean existsByProjeto_Id(UUID projetoId);

  @Override
  @EntityGraph(attributePaths = "projeto")
  java.util.Optional<Modulo> findById(UUID id);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);

  boolean existsBySlugAndProjeto_Id(String slug, UUID projetoId);

  boolean existsBySlugAndProjeto_IdAndIdNot(String slug, UUID projetoId, UUID id);

  @EntityGraph(attributePaths = "projeto")
  List<Modulo> findAllByOrderByProjeto_NomeAscOrdemAscNomeAsc();

  @EntityGraph(attributePaths = "projeto")
  List<Modulo> findByProjeto_IdOrderByOrdemAscNomeAsc(UUID projetoId);
}
