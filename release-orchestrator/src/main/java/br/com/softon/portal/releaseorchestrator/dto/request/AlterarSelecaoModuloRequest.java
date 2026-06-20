package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarSelecaoModuloRequest(@NotNull Boolean selecionado) {}
