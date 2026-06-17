package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusReleaseRequest(
        @NotNull ReleaseStatus status,
        String observacao
) {}
