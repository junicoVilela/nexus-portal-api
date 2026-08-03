package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.PaginaAnexo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaAnexoResponse(
    UUID id,
    UUID paginaId,
    String paginaTitulo,
    String nomeOriginal,
    String contentType,
    long tamanhoBytes,
    OffsetDateTime createdAt,
    String createdBy,
    String downloadUrl) {
  public static PaginaAnexoResponse from(PaginaAnexo anexo) {
    return new PaginaAnexoResponse(
        anexo.getId(),
        anexo.getPagina().getId(),
        anexo.getPagina().getTitulo(),
        anexo.getNomeOriginal(),
        anexo.getContentType(),
        anexo.getTamanhoBytes(),
        anexo.getCreatedAt(),
        anexo.getCreatedBy(),
        "/paginas/" + anexo.getPagina().getId() + "/anexos/" + anexo.getId() + "/download");
  }
}
