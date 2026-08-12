package com.nexus.portal.ai.dto.response;

import java.util.UUID;

public record AiTemplateCandidatoResponse(
    UUID templateId,
    String codigo,
    String nome,
    String descricao,
    double confianca,
    String motivo) {
}
