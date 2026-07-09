package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.HistoricoLogin;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HistoricoLoginResponse(
    UUID id,
    UUID usuarioId,
    String loginInformado,
    String ipOrigem,
    String userAgent,
    boolean sucesso,
    String motivoFalha,
    OffsetDateTime createdAt) {

  public static HistoricoLoginResponse from(HistoricoLogin h) {
    return new HistoricoLoginResponse(
        h.getId(), h.getUsuarioId(), h.getLoginInformado(), h.getIpOrigem(),
        h.getUserAgent(), h.isSucesso(), h.getMotivoFalha(), h.getCreatedAt());
  }
}
