package com.nexus.portal.ai.dto.response;

public record AiComponenteCandidatoResponse(
    String id,
    String nome,
    String descricao,
    String categoria,
    String visual,
    String necessidade,
    boolean obrigatorio,
    String motivo) {
}
