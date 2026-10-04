package com.nexus.portal.docflow.dto.request;

import com.nexus.portal.docflow.entity.TipoPagina;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaginaRequest(
    @NotBlank String titulo,
    String slug,
    @NotBlank String codigoTela,
    String resumo,
    String conteudoHtml,
    Integer ordem,
    Boolean ativo,
    @NotNull UUID moduloId,
    UUID parentId,
    Long version,
    UUID templateOrigemId,
    Integer templateOrigemVersao,
    /** Nulo = mantém o atual (ARTIGO na criação). */
    TipoPagina tipo) {

  public PaginaRequest(String titulo, String slug, String codigoTela, String resumo, String conteudoHtml,
      Integer ordem, Boolean ativo, UUID moduloId, UUID parentId, Long version, UUID templateOrigemId,
      Integer templateOrigemVersao) {
    this(titulo, slug, codigoTela, resumo, conteudoHtml, ordem, ativo, moduloId, parentId, version,
        templateOrigemId, templateOrigemVersao, null);
  }

  public PaginaRequest(String titulo, String slug, String codigoTela, String resumo, String conteudoHtml,
      Integer ordem, Boolean ativo, UUID moduloId, UUID parentId, Long version) {
    this(titulo, slug, codigoTela, resumo, conteudoHtml, ordem, ativo, moduloId, parentId, version, null, null);
  }
}
