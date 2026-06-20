package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContratarProdutoRequest(
    @NotNull UUID produtoId,
    @NotNull AmbientePadrao ambiente) {}
