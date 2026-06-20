package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.StatusProximaEntrega;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusProximaEntregaRequest(@NotNull StatusProximaEntrega status) {}
