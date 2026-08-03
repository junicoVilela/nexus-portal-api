package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PublicacaoRequest(
    @NotNull UUID clienteId,
    @NotBlank String versao,
    String observacao) {
}
