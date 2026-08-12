package com.nexus.portal.ai.service;

import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.dto.response.AiTemplateCandidatoResponse;
import com.nexus.portal.ai.dto.response.AiTemplateRecomendacaoResponse;
import com.nexus.portal.ai.integration.docflow.AiTemplateSelector;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiTemplateRecomendacaoService {

  private final DocFlowAiBridge docFlowAiBridge;

  public AiTemplateRecomendacaoService(DocFlowAiBridge docFlowAiBridge) {
    this.docFlowAiBridge = docFlowAiBridge;
  }

  public AiTemplateRecomendacaoResponse recomendar(AiTemplateRecomendacaoRequest request) {
    List<AiTemplateCandidatoResponse> candidatos = docFlowAiBridge
        .recomendarTemplates(request.projetoId(), request.clienteId(), request.briefing())
        .stream()
        .map(AiTemplateRecomendacaoService::mapear)
        .toList();
    AiTemplateCandidatoResponse recomendado = candidatos.isEmpty() ? null : candidatos.get(0);
    boolean exigeConfirmacao = recomendado == null
        || recomendado.confianca() < AiTemplateSelector.CONFIANCA_AUTO_SELECAO;
    return new AiTemplateRecomendacaoResponse(recomendado, candidatos, exigeConfirmacao);
  }

  private static AiTemplateCandidatoResponse mapear(
      AiTemplateSelector.Recomendacao recomendacao) {
    var template = recomendacao.template();
    return new AiTemplateCandidatoResponse(
        template.getId(),
        template.getCodigo(),
        template.getNome(),
        template.getDescricao(),
        recomendacao.confianca(),
        recomendacao.motivo());
  }
}
