package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiAtualizarComposicaoDocumentoRequest(
    @NotNull @PositiveOrZero Long version,
    @NotEmpty @Size(min = 3, max = 12) List<@NotNull String> componentesSelecionados) {}
