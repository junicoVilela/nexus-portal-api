package com.nexus.portal.ai.dto.response;

public record AiQualidadeItemResponse(
    String codigo,
    String titulo,
    String descricao,
    boolean ok,
    String severidade) {
}
