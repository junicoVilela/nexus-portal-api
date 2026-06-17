package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusProdutoRequest(@NotNull Boolean ativo) {}
