package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContratarProdutoRequest(
    @NotNull UUID produtoId,
    @NotNull AmbientePadrao ambiente) {}
