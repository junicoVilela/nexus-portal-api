package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.PaginaTemplateVersao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaginaTemplateVersaoRepository extends JpaRepository<PaginaTemplateVersao, UUID> {
  List<PaginaTemplateVersao> findByTemplate_IdOrderByNumeroDesc(UUID templateId);

  Optional<PaginaTemplateVersao> findByTemplate_IdAndNumero(UUID templateId, int numero);
}
