package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import jakarta.validation.constraints.NotNull;

public record AtualizarClienteProdutoRequest(
    @NotNull AmbientePadrao ambiente,
    Boolean ativo) {}
