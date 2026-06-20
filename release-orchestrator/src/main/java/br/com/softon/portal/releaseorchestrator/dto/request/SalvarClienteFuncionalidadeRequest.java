package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.OrigemFuncionalidade;
import jakarta.validation.constraints.NotNull;

public record SalvarClienteFuncionalidadeRequest(
    @NotNull Boolean habilitada,
    OrigemFuncionalidade origem) {}
