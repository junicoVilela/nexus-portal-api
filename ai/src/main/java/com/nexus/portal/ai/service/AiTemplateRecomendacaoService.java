package com.nexus.portal.ai.service;

import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.dto.response.AiComponenteCandidatoResponse;
import com.nexus.portal.ai.dto.response.AiTemplateCandidatoResponse;
import com.nexus.portal.ai.dto.response.AiTemplateRecomendacaoResponse;
import com.nexus.portal.ai.integration.docflow.AiTemplateSelector;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintSecaoResponse;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiTemplateRecomendacaoService {

  private final DocFlowAiBridge docFlowAiBridge;
  private final AiComponenteRetriever componenteRetriever;

  public AiTemplateRecomendacaoService(
      DocFlowAiBridge docFlowAiBridge,
      AiComponenteRetriever componenteRetriever) {
    this.docFlowAiBridge = docFlowAiBridge;
    this.componenteRetriever = componenteRetriever;
  }

  public AiTemplateRecomendacaoResponse recomendar(AiTemplateRecomendacaoRequest request) {
    List<AiTemplateCandidatoResponse> candidatos = candidatos(request);
    AiTemplateCandidatoResponse recomendado = candidatos.isEmpty() ? null : candidatos.get(0);
    boolean exigeConfirmacao = request.templateId() == null && (recomendado == null
        || recomendado.confianca() < AiTemplateSelector.CONFIANCA_AUTO_SELECAO);
    PaginaBlueprintResponse blueprint = recomendado == null
        ? null
        : docFlowAiBridge.buscarBlueprint(recomendado.codigo()).orElse(null);
    List<PaginaBlocoResponse> biblioteca = docFlowAiBridge.listarBlocos();
    List<AiComponenteCandidatoResponse> componentes = recomendado == null
        ? List.of()
        : componenteRetriever
            .recuperar(recomendado.codigo(), request.briefing(), biblioteca, blueprint)
            .stream()
            .map(bloco -> mapearComponente(bloco, blueprint))
            .toList();
    return new AiTemplateRecomendacaoResponse(
        recomendado,
        candidatos,
        exigeConfirmacao,
        blueprint == null ? null : blueprint.id(),
        blueprint == null ? null : blueprint.nome(),
        biblioteca.size(),
        componentes);
  }

  public List<String> validarComponentes(
      String briefing,
      UUID projetoId,
      UUID clienteId,
      UUID templateId,
      List<String> selecionados) {
    if (selecionados == null || selecionados.isEmpty()) {
      return List.of();
    }
    Set<String> unicos = new LinkedHashSet<>(selecionados);
    if (unicos.size() != selecionados.size()) {
      throw new BusinessException("A composição contém componentes repetidos.");
    }
    if (unicos.size() < 3 || unicos.size() > 12) {
      throw new BusinessException("Selecione de 3 a 12 componentes para compor a página.");
    }
    AiTemplateRecomendacaoResponse plano = recomendar(
        new AiTemplateRecomendacaoRequest(briefing, projetoId, clienteId, templateId));
    Set<String> permitidos = docFlowAiBridge.listarBlocos().stream()
        .map(PaginaBlocoResponse::id)
        .collect(java.util.stream.Collectors.toSet());
    if (!permitidos.containsAll(unicos)) {
      throw new BusinessException("A composição contém um componente indisponível no catálogo.");
    }
    Set<String> obrigatorios = plano.componentes().stream()
        .filter(AiComponenteCandidatoResponse::obrigatorio)
        .map(AiComponenteCandidatoResponse::id)
        .collect(java.util.stream.Collectors.toSet());
    if (!unicos.containsAll(obrigatorios)) {
      throw new BusinessException("Mantenha os componentes essenciais do blueprint selecionado.");
    }
    return List.copyOf(selecionados);
  }

  private List<AiTemplateCandidatoResponse> candidatos(AiTemplateRecomendacaoRequest request) {
    if (request.templateId() != null) {
      return docFlowAiBridge.buscarTemplate(
              request.templateId(), request.projetoId(), request.clienteId(), request.briefing())
          .map(template -> List.of(mapearEscolhido(template)))
          .orElseGet(List::of);
    }
    return docFlowAiBridge
        .recomendarTemplates(request.projetoId(), request.clienteId(), request.briefing())
        .stream()
        .map(AiTemplateRecomendacaoService::mapear)
        .toList();
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

  private static AiTemplateCandidatoResponse mapearEscolhido(PaginaTemplate template) {
    return new AiTemplateCandidatoResponse(
        template.getId(),
        template.getCodigo(),
        template.getNome(),
        template.getDescricao(),
        1,
        "Modelo escolhido para esta composição.");
  }

  private static AiComponenteCandidatoResponse mapearComponente(
      PaginaBlocoResponse bloco,
      PaginaBlueprintResponse blueprint) {
    PaginaBlueprintSecaoResponse secao = blueprint == null
        ? null
        : blueprint.secoes().stream()
            .filter(item -> item.componenteId().equals(bloco.id())
                || item.alternativas().contains(bloco.id()))
            .findFirst()
            .orElse(null);
    String necessidade = secao == null ? "CONTEXTUAL" : secao.necessidade();
    boolean obrigatorio = "OBRIGATORIA".equals(necessidade);
    String motivo = switch (necessidade) {
      case "OBRIGATORIA" -> "Essencial para a estrutura editorial desta página.";
      case "RECOMENDADA" -> "Recomendado pelo blueprint para orientar o usuário.";
      case "OPCIONAL" -> "Incluído porque o briefing apresenta conteúdo relacionado.";
      default -> "Identificado pelos termos e pela finalidade descrita no briefing.";
    };
    return new AiComponenteCandidatoResponse(
        bloco.id(),
        bloco.nome(),
        bloco.descricao(),
        bloco.categoria(),
        bloco.visual(),
        necessidade,
        obrigatorio,
        motivo);
  }
}
