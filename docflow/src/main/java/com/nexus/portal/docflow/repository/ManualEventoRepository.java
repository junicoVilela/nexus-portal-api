package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.ManualEvento;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualEventoRepository extends JpaRepository<ManualEvento, UUID> {

  long countByTipoAndCreatedAtAfter(ManualEvento.Tipo tipo, OffsetDateTime desde);

  /** Termos mais buscados sem resultado: [termo, ocorrências]. */
  @Query("""
      select lower(trim(e.termo)), count(e) from ManualEvento e
       where e.tipo = com.nexus.portal.docflow.entity.ManualEvento.Tipo.BUSCA_SEM_RESULTADO
         and e.createdAt > :desde
       group by lower(trim(e.termo))
       order by count(e) desc, lower(trim(e.termo))
      """)
  List<Object[]> termosSemResultado(@Param("desde") OffsetDateTime desde, Pageable limite);

  @Modifying
  long deleteByCreatedAtBefore(OffsetDateTime limite);
}
