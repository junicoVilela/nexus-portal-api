package br.com.softon.rbac.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UsuarioGruposRequest(
    @NotNull(message = "Lista de grupos é obrigatória")
    List<UUID> grupoIds) {
}
