package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.AuditoriaEvento;
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
