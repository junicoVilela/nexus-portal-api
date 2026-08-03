package com.nexus.identityaccess.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AcessoTemporarioRequest(
    @NotNull UUID usuarioId,
    UUID grupoAcessoId,
    UUID permissaoId,
    UUID escopoAcessoId,
    @NotNull OffsetDateTime inicioEm,
    @NotNull OffsetDateTime fimEm,
    String justificativa) {
}
