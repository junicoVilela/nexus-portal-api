package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusClienteRequest(@NotNull Boolean ativo) {}
