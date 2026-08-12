package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiTemplateRecomendacaoRequest(
    @NotBlank @Size(min = 20, max = 50_000) String briefing,
    UUID projetoId,
    UUID clienteId) {
}
