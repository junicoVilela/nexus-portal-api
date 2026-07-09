package br.com.softon.rbac.dto.request;

import jakarta.validation.constraints.NotNull;

public record BloqueioUsuarioRequest(
    @NotNull(message = "bloqueado é obrigatório") Boolean bloqueado) {
}
