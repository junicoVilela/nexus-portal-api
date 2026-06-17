package br.com.softon.portal.docflow.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusGrupoRequest(
    @NotNull(message = "Status é obrigatório")
    Boolean ativo) {
}
