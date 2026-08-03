package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.OrigemFuncionalidade;
import jakarta.validation.constraints.NotNull;

public record SalvarClienteFuncionalidadeRequest(
    @NotNull Boolean habilitada,
    OrigemFuncionalidade origem) {}
