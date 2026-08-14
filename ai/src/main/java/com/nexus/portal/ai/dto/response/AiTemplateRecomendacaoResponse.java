package com.nexus.portal.ai.dto.response;

import java.util.List;

public record AiTemplateRecomendacaoResponse(
    AiTemplateCandidatoResponse recomendado,
    List<AiTemplateCandidatoResponse> candidatos,
    boolean exigeConfirmacao,
    String blueprintId,
    String blueprintNome,
    int totalBiblioteca,
    List<AiComponenteCandidatoResponse> componentes) {

  public AiTemplateRecomendacaoResponse(
      AiTemplateCandidatoResponse recomendado,
      List<AiTemplateCandidatoResponse> candidatos,
      boolean exigeConfirmacao) {
    this(recomendado, candidatos, exigeConfirmacao, null, null, 0, List.of());
  }
}
