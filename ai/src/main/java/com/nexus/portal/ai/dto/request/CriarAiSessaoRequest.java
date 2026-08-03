package com.nexus.portal.ai.dto.request;

import com.nexus.portal.ai.entity.AiObjetivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CriarAiSessaoRequest(
    @NotNull AiObjetivo objetivo,
    @NotBlank @Size(min = 40, max = 50_000) String briefing,
    UUID projetoId,
    UUID moduloId,
    UUID clienteId,
    UUID templateId,
    UUID paginaId) {
}
