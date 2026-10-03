package com.nexus.portal.ai.dto.response;

import java.util.UUID;

/** Sessão de ajuste criada e o job enfileirado; o acompanhamento segue por SSE/polling da sessão. */
public record AiAjustePaginaResponse(UUID sessaoId, AiJobResponse job) {
}
