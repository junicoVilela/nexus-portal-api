package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.Sessao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SessaoResponse(
    UUID id,
    UUID usuarioId,
    String ipOrigem,
    String userAgent,
    boolean ativa,
    boolean revogada,
    String motivoEncerramento,
    OffsetDateTime iniciadaEm,
    OffsetDateTime encerradaEm,
    OffsetDateTime expiraEm) {

  public static SessaoResponse from(Sessao s) {
    return new SessaoResponse(
        s.getId(), s.getUsuarioId(), s.getIpOrigem(), s.getUserAgent(),
        s.isAtiva(), s.isRevogada(), s.getMotivoEncerramento(),
        s.getIniciadaEm(), s.getEncerradaEm(), s.getExpiraEm());
  }
}
