package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusReleaseRequest(
        @NotNull ReleaseStatus status,
        String observacao
) {}
