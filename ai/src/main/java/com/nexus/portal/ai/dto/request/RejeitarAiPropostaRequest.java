package com.nexus.portal.ai.dto.request;

import com.nexus.portal.ai.entity.AiCategoriaRejeicao;
import jakarta.validation.constraints.Size;

/** Os dois campos são opcionais: rejeitar sem explicar continua permitido. */
public record RejeitarAiPropostaRequest(AiCategoriaRejeicao categoria, @Size(max = 500) String motivo) {
}
