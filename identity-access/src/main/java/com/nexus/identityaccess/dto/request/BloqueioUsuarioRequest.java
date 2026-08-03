package com.nexus.identityaccess.dto.request;

import jakarta.validation.constraints.NotNull;

public record BloqueioUsuarioRequest(
    @NotNull(message = "bloqueado é obrigatório") Boolean bloqueado) {
}
