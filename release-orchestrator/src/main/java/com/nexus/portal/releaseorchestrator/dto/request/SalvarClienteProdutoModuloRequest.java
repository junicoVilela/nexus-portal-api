package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.Size;

public record SalvarClienteProdutoModuloRequest(
    @Size(max = 80) String versaoAtual,
    Boolean ativo) {}
