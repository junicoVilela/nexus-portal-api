package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
    @NotBlank String nome,
    String slug,
    Boolean ativo,
    String temaCorPrimaria,
    String temaCorFundo) {
}
