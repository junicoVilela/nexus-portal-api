package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.Size;

public record RejeitarAiPropostaRequest(@Size(max = 500) String motivo) {
}
