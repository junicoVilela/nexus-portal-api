package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.PaginaRevisao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaginaRevisaoRepository extends JpaRepository<PaginaRevisao, UUID> {
  List<PaginaRevisao> findByPagina_IdOrderByNumeroDesc(UUID paginaId);

  Page<PaginaRevisao> findByPagina_Id(UUID paginaId, Pageable pageable);

  int countByPagina_Id(UUID paginaId);
}
