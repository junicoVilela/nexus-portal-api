package com.nexus.identityaccess.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record GrupoUsuariosRequest(
    @NotNull(message = "Lista de usuários é obrigatória")
    List<UUID> usuarioIds) {
}
