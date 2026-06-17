package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.AuditoriaEvento;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditoriaResponse(
    UUID id,
    String entidade,
    UUID entidadeId,
    String acao,
    String descricao,
    OffsetDateTime createdAt,
    String createdBy) {

  public static AuditoriaResponse from(AuditoriaEvento evento) {
    return new AuditoriaResponse(
        evento.getId(),
        evento.getEntidade(),
        evento.getEntidadeId(),
        evento.getAcao(),
        evento.getDescricao(),
        evento.getCreatedAt(),
        evento.getCreatedBy());
  }
}
