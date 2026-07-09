package br.com.softon.rbac.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GrupoPermissoesRequest(
    @NotNull(message = "Lista de permissões é obrigatória")
    List<String> permissoes) {
}
