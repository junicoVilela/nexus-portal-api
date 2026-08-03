package com.nexus.identityaccess.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusGrupoRequest(
    @NotNull(message = "Status é obrigatório")
    Boolean ativo) {
}
