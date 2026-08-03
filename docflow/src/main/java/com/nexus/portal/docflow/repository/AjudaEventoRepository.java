package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.AjudaEvento;
import com.nexus.portal.docflow.entity.TipoAjudaEvento;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AjudaEventoRepository extends JpaRepository<AjudaEvento, UUID> {
  long countByCreatedAtGreaterThanEqual(OffsetDateTime desde);

  long countByTipoAndCreatedAtGreaterThanEqual(TipoAjudaEvento tipo, OffsetDateTime desde);

  long deleteByCreatedAtBefore(OffsetDateTime limite);

  @Query("""
      select count(e)
      from AjudaEvento e
      where e.tipo = :tipo and e.createdAt >= :desde
        and ((:sessaoId is null and e.sessaoId is null) or e.sessaoId = :sessaoId)
        and ((:conteudoCodigo is null and e.conteudoCodigo is null) or e.conteudoCodigo = :conteudoCodigo)
        and ((:termo is null and e.termo is null) or e.termo = :termo)
        and ((:rota is null and e.rota is null) or e.rota = :rota)
      """)
  long contarEventoEquivalente(@Param("tipo") TipoAjudaEvento tipo,
      @Param("sessaoId") String sessaoId, @Param("conteudoCodigo") String conteudoCodigo,
      @Param("termo") String termo, @Param("rota") String rota,
      @Param("desde") OffsetDateTime desde);

  @Query("""
      select e.conteudoCodigo, count(e)
      from AjudaEvento e
      where e.createdAt >= :desde and e.tipo = :tipo and e.conteudoCodigo is not null
      group by e.conteudoCodigo
      order by count(e) desc
      """)
  List<Object[]> conteudosMaisAcessados(@Param("desde") OffsetDateTime desde,
      @Param("tipo") TipoAjudaEvento tipo, Pageable pageable);

  @Query("""
      select lower(e.termo), count(e)
      from AjudaEvento e
      where e.createdAt >= :desde and e.tipo in :tipos and e.termo is not null and e.termo <> ''
      group by lower(e.termo)
      order by count(e) desc
      """)
  List<Object[]> buscasFrequentes(@Param("desde") OffsetDateTime desde,
      @Param("tipos") List<TipoAjudaEvento> tipos, Pageable pageable);
}
