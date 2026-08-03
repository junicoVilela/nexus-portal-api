package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProjetoRequest(
    @NotBlank String nome,
    String slug,
    String descricao,
    Boolean ativo) {
}
