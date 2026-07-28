package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.StatusPagina;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaginaRepository extends JpaRepository<Pagina, UUID>, JpaSpecificationExecutor<Pagina> {
  boolean existsByModulo_Id(UUID moduloId);

  boolean existsByParent_Id(UUID parentId);

  @Override
  @EntityGraph(attributePaths = {"modulo", "modulo.projeto", "parent", "parent.modulo", "parent.modulo.projeto"})
  Optional<Pagina> findById(UUID id);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);

  boolean existsByCodigoTela(String codigoTela);

  boolean existsByCodigoTelaAndIdNot(String codigoTela, UUID id);

  long countByTemplateOrigemId(UUID templateId);

  long countByTemplateOrigemIdAndTemplateOrigemVersao(UUID templateId, Integer versao);

  long countByAtivoTrue();

  long countByStatusAndAtivoTrueAndPublishedAtBefore(StatusPagina status, OffsetDateTime limite);

  @Query("select count(p) from Pagina p where p.ativo = true and (p.resumo is null or length(trim(p.resumo)) < 30)")
  long countSemResumoEditorial();

  @Query(value = """
      SELECT p.* FROM tb_pagina p
      WHERE p.search_vector @@ plainto_tsquery('portuguese', :termo)
        AND p.ativo = true
      ORDER BY p.titulo ASC
      """, nativeQuery = true)
  List<Pagina> buscaFullText(@Param("termo") String termo);

  @Query("""
      select p from Pagina p
      join fetch p.modulo m
      left join fetch p.parent pa
      left join fetch pa.modulo
      join fetch m.projeto pr
      where p.status = :status
        and p.ativo = true
        and m.ativo = true
        and pr.ativo = true
      order by pr.nome asc, m.ordem asc, p.ordem asc, p.titulo asc
      """)
  List<Pagina> findAtivasByStatusWithModulo(@Param("status") StatusPagina status);

  @Query("select p.status, count(p) from Pagina p where p.ativo = true group by p.status")
  List<Object[]> contarPorStatusAgrupado();
}
