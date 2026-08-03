package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AiMensagemRequest(
    @NotBlank @Size(max = 20_000) String conteudo,
    Map<String, String> respostas) {
}
