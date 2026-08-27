package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.PaginaSnippet;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaSnippetResponse(
    UUID id,
    String codigo,
    String referencia,
    String titulo,
    String descricao,
    String conteudoHtml,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {

  public static PaginaSnippetResponse from(PaginaSnippet snippet) {
    return new PaginaSnippetResponse(
        snippet.getId(),
        snippet.getCodigo(),
        "{{snippet:" + snippet.getCodigo() + "}}",
        snippet.getTitulo(),
        snippet.getDescricao(),
        snippet.getConteudoHtml(),
        snippet.isAtivo(),
        snippet.getCreatedAt(),
        snippet.getUpdatedAt(),
        snippet.getCreatedBy(),
        snippet.getUpdatedBy());
  }
}
