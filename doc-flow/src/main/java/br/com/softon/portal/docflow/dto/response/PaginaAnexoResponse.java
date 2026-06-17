package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.PaginaAnexo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaAnexoResponse(
    UUID id,
    UUID paginaId,
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
        anexo.getNomeOriginal(),
        anexo.getContentType(),
        anexo.getTamanhoBytes(),
        anexo.getCreatedAt(),
        anexo.getCreatedBy(),
        "/api/paginas/" + anexo.getPagina().getId() + "/anexos/" + anexo.getId() + "/download");
  }
}
