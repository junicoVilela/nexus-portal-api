package com.nexus.portal.docflow.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PaginaTemplateAplicacaoResponse(
    UUID templateId,
    int versao,
    String conteudoHtml,
    Map<String, String> variaveisResolvidas,
    List<String> variaveisPendentes) {
}
