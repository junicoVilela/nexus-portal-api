package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusTemplateRequest(@NotNull Boolean ativo) {}
