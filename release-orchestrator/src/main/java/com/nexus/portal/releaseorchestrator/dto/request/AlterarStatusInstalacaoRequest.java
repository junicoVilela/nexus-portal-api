package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusInstalacaoRequest(@NotNull StatusInstalacao status) {}
