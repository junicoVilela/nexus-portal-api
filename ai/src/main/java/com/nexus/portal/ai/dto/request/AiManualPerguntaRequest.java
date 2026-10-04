package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiManualPerguntaRequest(@NotBlank @Size(min = 3, max = 500) String pergunta) {
}
