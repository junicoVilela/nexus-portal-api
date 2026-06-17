package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.PaginaRevisao;
import br.com.softon.portal.docflow.entity.StatusPagina;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaRevisaoResponse(
    UUID id,
    int numero,
    String titulo,
    StatusPagina status,
    OffsetDateTime createdAt,
    String createdBy) {
  public static PaginaRevisaoResponse from(PaginaRevisao revisao) {
    return new PaginaRevisaoResponse(revisao.getId(), revisao.getNumero(), revisao.getTitulo(),
        revisao.getStatus(), revisao.getCreatedAt(), revisao.getCreatedBy());
  }
}
