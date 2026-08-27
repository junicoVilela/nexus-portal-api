package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.PaginaRevisao;
import com.nexus.portal.docflow.entity.TipoRevisaoPagina;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaginaRevisaoRepository extends JpaRepository<PaginaRevisao, UUID> {

  Optional<PaginaRevisao> findFirstByPagina_IdAndTipoOrderByNumeroDesc(
      UUID paginaId, TipoRevisaoPagina tipo);

  List<PaginaRevisao> findByPagina_IdOrderByNumeroDesc(UUID paginaId);

  Page<PaginaRevisao> findByPagina_Id(UUID paginaId, Pageable pageable);

  int countByPagina_Id(UUID paginaId);
}
