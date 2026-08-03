package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.StatusProximaEntrega;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusProximaEntregaRequest(@NotNull StatusProximaEntrega status) {}
