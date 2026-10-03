package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Página salva a partir da proposta aplicada no editor. */
public record AiVincularPaginaRequest(@NotNull UUID paginaId) {
}
