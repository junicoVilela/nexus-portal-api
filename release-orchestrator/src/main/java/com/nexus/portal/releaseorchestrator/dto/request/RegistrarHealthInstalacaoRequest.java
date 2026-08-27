package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import jakarta.validation.constraints.Size;

public record RegistrarHealthInstalacaoRequest(
    HealthInstalacao health,
    @Size(max = 80) String versaoAtual,
    @Size(max = 4000) String ultimoErro) {}
