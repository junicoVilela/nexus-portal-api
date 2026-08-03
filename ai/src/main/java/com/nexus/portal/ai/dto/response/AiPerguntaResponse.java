package com.nexus.portal.ai.dto.response;

import java.util.List;

public record AiPerguntaResponse(
    String id,
    String texto,
    List<String> opcoes,
    boolean obrigatoria) {
}
