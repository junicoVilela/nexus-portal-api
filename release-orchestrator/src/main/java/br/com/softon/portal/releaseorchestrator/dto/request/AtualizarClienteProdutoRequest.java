package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import jakarta.validation.constraints.NotNull;

public record AtualizarClienteProdutoRequest(
    @NotNull AmbientePadrao ambiente,
    Boolean ativo) {}
