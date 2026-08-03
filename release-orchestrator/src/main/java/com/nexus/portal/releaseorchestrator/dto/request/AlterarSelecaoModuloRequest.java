package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarSelecaoModuloRequest(@NotNull Boolean selecionado) {}
