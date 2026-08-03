package com.nexus.identityaccess.dto.response;

import com.nexus.identityaccess.entity.AcessoTemporario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AcessoTemporarioResponse(
    UUID id,
    UUID usuarioId,
    UUID grupoAcessoId,
    UUID permissaoId,
    UUID escopoAcessoId,
    OffsetDateTime inicioEm,
    OffsetDateTime fimEm,
    String status,
    String justificativa,
    OffsetDateTime criadoEm,
    OffsetDateTime revogadoEm) {

  public static AcessoTemporarioResponse from(AcessoTemporario a) {
    return new AcessoTemporarioResponse(
        a.getId(), a.getUsuarioId(),
        a.getGrupoId(), a.getPermissaoId(), a.getEscopoId(),
        a.getInicioEm(), a.getFimEm(),
        a.status(),
        a.getJustificativa(),
        a.getCreatedAt(),
        a.getRevogadoEm());
  }
}
