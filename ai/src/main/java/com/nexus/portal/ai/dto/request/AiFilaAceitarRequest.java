package com.nexus.portal.ai.dto.request;

import java.util.UUID;

/** Aceitar página nova da fila: módulo e pai opcionais (padrão: módulo do repositório). */
public record AiFilaAceitarRequest(UUID moduloId, UUID parentId) {
}
