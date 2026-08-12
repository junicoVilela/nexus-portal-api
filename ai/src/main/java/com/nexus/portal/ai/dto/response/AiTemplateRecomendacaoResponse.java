package com.nexus.portal.ai.dto.response;

import java.util.List;

public record AiTemplateRecomendacaoResponse(
    AiTemplateCandidatoResponse recomendado,
    List<AiTemplateCandidatoResponse> candidatos,
    boolean exigeConfirmacao) {
}
