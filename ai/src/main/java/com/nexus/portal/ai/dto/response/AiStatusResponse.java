package com.nexus.portal.ai.dto.response;

public record AiStatusResponse(
    boolean enabled,
    boolean prontoParaGerar,
    String provider,
    String model,
    String mensagem) {
}
