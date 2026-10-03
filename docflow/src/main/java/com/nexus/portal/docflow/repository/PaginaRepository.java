package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.StatusPagina;
import java.time.OffsetDateTime;
import java.util.Collection;
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

  Optional<Pagina> findByCodigoTela(String codigoTela);

  boolean existsByCodigoTelaAndIdNot(String codigoTela, UUID id);

  long countByTemplateOrigemId(UUID templateId);

  long countByTemplateOrigemIdAndTemplateOrigemVersao(UUID templateId, Integer versao);

  /** Uma consulta para a listagem inteira de modelos, em vez de um count por linha. */
  @Query("""
      select p.templateOrigemId, count(p) from Pagina p
      where p.templateOrigemId in :templateIds
      group by p.templateOrigemId
      """)
  List<Object[]> contarPorTemplateOrigem(@Param("templateIds") Collection<UUID> templateIds);

  @Query("""
      select p.templateOrigemVersao, count(p) from Pagina p
      where p.templateOrigemId = :templateId and p.templateOrigemVersao is not null
      group by p.templateOrigemVersao
      """)
  List<Object[]> contarPorVersaoDoTemplate(@Param("templateId") UUID templateId);

  long countByAtivoTrue();

  long countByStatusAndAtivoTrueAndPublishedAtBefore(StatusPagina status, OffsetDateTime limite);

  @Query("select count(p) from Pagina p where p.ativo = true and (p.resumo is null or length(trim(p.resumo)) < 30)")
  long countSemResumoEditorial();

  /**
   * Ids que casam com o termo no índice GIN. A listagem continua sendo montada
   * pela Specification (filtros, ordenação e paginação); daqui sai só o
   * conjunto de ids que o índice resolveu.
   */
  @Query(value = """
      SELECT p.id FROM tb_pagina p
      WHERE p.search_vector @@ plainto_tsquery('portuguese', :termo)
      """, nativeQuery = true)
  List<UUID> buscarIdsPorTexto(@Param("termo") String termo);

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

  /** Páginas ativas cujo conteúdo cita o texto informado (referência de trecho). */
  @Query("select count(p) from Pagina p where p.ativo = true and p.conteudoHtml like concat('%', :trecho, '%')")
  long contarPaginasQueCitam(@Param("trecho") String trecho);

  List<Pagina> findByParent_Id(UUID parentId);

  @EntityGraph(attributePaths = {"modulo", "modulo.projeto", "parent"})
  org.springframework.data.domain.Page<Pagina> findByRevisorUsernameAndStatus(
      String revisorUsername, StatusPagina status, org.springframework.data.domain.Pageable pageable);
}
