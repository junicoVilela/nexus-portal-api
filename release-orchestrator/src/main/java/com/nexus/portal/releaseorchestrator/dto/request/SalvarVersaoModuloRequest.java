package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SalvarVersaoModuloRequest(
    @NotBlank @Size(max = 80) String versao) {}
