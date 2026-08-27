package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ExecutarDeployLoteRequest(
    @NotNull UUID entregaId,
    Boolean forcar,
    ModoDeploy modo) {}
