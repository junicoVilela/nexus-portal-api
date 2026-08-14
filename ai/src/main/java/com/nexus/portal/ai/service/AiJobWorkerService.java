package com.nexus.portal.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.response.AiQualidadeItemResponse;
import com.nexus.portal.ai.entity.AiJobEtapa;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.integration.docflow.AiTemplateSelector;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.prompt.AiPromptBuilder;
import com.nexus.portal.ai.provider.LlmCompletion;
import com.nexus.portal.ai.provider.LlmProvider;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.service.AiJobLifecycleService.ContextoExecucao;
import com.nexus.portal.ai.service.AiJobLifecycleService.ResultadoConclusao;
import com.nexus.portal.ai.service.AiJobLifecycleService.ResultadoGeracao;
import com.nexus.portal.docflow.dto.response.PaginaTemplateAplicacaoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobWorkerService {

  private final AiMensagemRepository mensagemRepository;
  private final AiJobLifecycleService lifecycleService;
  private final DocFlowAiBridge docFlowAiBridge;
  private final LlmProvider llmProvider;
  private final AiProperties properties;
  private final AiHtmlSanitizer htmlSanitizer;
  private final AiTriagemService triagemService;
  private final AiComponenteRetriever componenteRetriever;
  private final AiPageSpecService pageSpecService;
  private final AiBriefingPageSpecEnricher briefingPageSpecEnricher;
  private final ObjectMapper objectMapper;

  @Async
  public void processar(UUID jobId) {
    var execucao = lifecycleService.iniciar(jobId, properties.model());
    if (execucao.isEmpty()) {
      return;
    }
    ContextoExecucao sessao = execucao.orElseThrow();
    try {
      Map<String, String> respostas = coletarContexto(sessao);
      Map<String, String> contexto = new LinkedHashMap<>(
          triagemService.avaliar(sessao.objetivo(), sessao.briefing(), respostas).contextoExtraido());
      contexto.putAll(respostas);
      String titulo = primeiroNaoVazio(
          contexto.get("titulo"), extrairTituloBriefing(sessao.briefing()), "Página gerada");
      String codigoTela = normalizarCodigoTela(
          primeiroNaoVazio(contexto.get("codigoTela"), "AI-DEMO"));
      String resumoHint = primeiroNaoVazio(
          contexto.get("resumo"), contexto.get("fluxo"), sessao.briefing());

      lifecycleService.avancar(jobId, AiJobEtapa.SELECIONANDO_ESTRUTURA, 30);

      PaginaTemplate template = docFlowAiBridge
          .buscarTemplate(
              sessao.templateId(),
              sessao.projetoId(),
              sessao.clienteId(),
              sessao.briefing())
          .orElse(null);

      String esqueleto = "";
      String templateCodigo = null;
      String templateNome = null;
      UUID templateId = null;
      Integer templateVersao = null;
      if (template != null) {
        templateId = template.getId();
        templateVersao = template.getVersaoAtual();
        templateCodigo = template.getCodigo();
        templateNome = template.getNome();
        PaginaTemplateAplicacaoResponse aplicado = docFlowAiBridge.aplicarTemplate(
            templateId,
            sessao.projetoId(),
            sessao.moduloId(),
            sessao.clienteId(),
            titulo,
            codigoTela);
        esqueleto = aplicado.conteudoHtml() == null ? "" : aplicado.conteudoHtml();
        templateVersao = aplicado.versao();
      }

      PaginaBlueprintResponse blueprint = docFlowAiBridge.buscarBlueprint(templateCodigo).orElse(null);
      List<PaginaBlocoResponse> catalogo = docFlowAiBridge.listarBlocos();
      List<PaginaBlocoResponse> candidatos = sessao.componentesSelecionados().isEmpty()
          ? componenteRetriever.recuperar(templateCodigo, sessao.briefing(), catalogo, blueprint)
          : componentesSelecionados(sessao.componentesSelecionados(), catalogo);
      lifecycleService.avancar(jobId, AiJobEtapa.GERANDO_CONTEUDO, 50);
      LlmCompletion completion;
      JsonNode json;
      AiPageSpec pageSpec = null;
      String htmlFinal;
      if (!candidatos.isEmpty()) {
        String system = AiPromptBuilder.systemGerarPageSpec();
        String user = AiPromptBuilder.userGerarPageSpec(
            titulo,
            codigoTela,
            truncar(resumoHint, 500),
            sessao.briefing(),
            contexto,
            templateCodigo,
            templateNome,
            blueprint,
            candidatos);
        completion = llmProvider.completarEstruturado(
            system, user, pageSpecService.schema(candidatos));
        try {
          json = extrairJson(completion.content());
        } catch (Exception ex) {
          log.warn(
              "ai.job.pagespec_fallback jobId={} motivo=json_invalido detalhe={}",
              jobId,
              ex.getMessage());
          json = objectMapper.createObjectNode()
              .put("titulo", titulo)
              .put("slug", slugify(titulo))
              .put("codigoTela", codigoTela)
              .put("resumo", truncar(resumoHint, 280));
        }
        try {
          pageSpec = pageSpecService.interpretar(json, candidatos, blueprint);
        } catch (IllegalArgumentException ex) {
          log.warn(
              "ai.job.pagespec_fallback jobId={} motivo={} componentes={}",
              jobId,
              ex.getMessage(),
              candidatos.stream().map(PaginaBlocoResponse::id).toList());
          pageSpec = pageSpecService.fallback(
              text(json, "titulo", titulo),
              text(json, "slug", slugify(titulo)),
              text(json, "codigoTela", codigoTela),
              text(json, "resumo", truncar(resumoHint, 280)),
              candidatos,
              blueprint);
        }
        if (!sessao.componentesSelecionados().isEmpty()) {
          pageSpec = pageSpecService.garantirComponentes(pageSpec, candidatos);
        }
        pageSpec = briefingPageSpecEnricher.enriquecer(pageSpec, candidatos, sessao.briefing());
        htmlFinal = htmlSanitizer.sanitizar(pageSpecService.renderizar(pageSpec));
      } else {
        // Compatibilidade defensiva: em operação normal o catálogo canônico nunca fica vazio.
        String system = AiPromptBuilder.systemGerarRascunho();
        String user = AiPromptBuilder.userGerarRascunho(
            titulo,
            codigoTela,
            truncar(resumoHint, 500),
            sessao.briefing(),
            contexto,
            truncar(esqueleto, 14_000),
            templateCodigo,
            templateNome);
        completion = llmProvider.completar(system, user);
        json = extrairJson(completion.content());
        String htmlLlm = htmlSanitizer.sanitizar(text(json, "conteudoHtml", esqueleto));
        htmlFinal = decidirHtmlBiblioteca(esqueleto, htmlLlm);
      }

      String tituloFinal = truncar(
          pageSpec == null ? text(json, "titulo", titulo) : pageSpec.titulo(), 200);
      String slugFinal = truncar(
          pageSpec == null ? text(json, "slug", slugify(tituloFinal)) : pageSpec.slug(), 200);
      String codigoFinal = truncar(
          normalizarCodigoTela(
              pageSpec == null ? text(json, "codigoTela", codigoTela) : pageSpec.codigoTela()),
          120);
      String resumoFinal = truncar(
          pageSpec == null ? text(json, "resumo", truncar(resumoHint, 280)) : pageSpec.resumo(),
          2_000);
      if (htmlFinal == null || htmlFinal.isBlank()) {
        throw new IllegalStateException("A PageSpec não produziu conteúdo utilizável.");
      }

      lifecycleService.avancar(jobId, AiJobEtapa.VALIDANDO_QUALIDADE, 80);

      ResultadoQualidade qualidade = docFlowAiBridge.avaliarQualidade(
          tituloFinal, codigoFinal, resumoFinal, htmlFinal);
      String qualidadeJson = objectMapper.writeValueAsString(Map.of(
          "aptoParaRevisao", qualidade.aptoParaRevisao(),
          "itens", qualidade.itens().stream()
              .map(i -> new AiQualidadeItemResponse(
                  i.codigo(), i.titulo(), i.descricao(), i.ok(), i.severidade().name()))
              .toList()));

      lifecycleService.avancar(jobId, AiJobEtapa.FINALIZANDO, 95);
      AiPropostaTipo tipo = sessao.objetivo() == AiObjetivo.ATUALIZAR_PAGINA
          ? AiPropostaTipo.ATUALIZACAO
          : AiPropostaTipo.NOVA;
      ResultadoConclusao conclusao = lifecycleService.concluir(jobId, new ResultadoGeracao(
          tipo,
          tituloFinal,
          slugFinal,
          codigoFinal,
          resumoFinal,
          htmlFinal,
          templateId,
          templateVersao,
          qualidadeJson,
          pageSpec == null ? null : objectMapper.writeValueAsString(pageSpec),
          completion.tokensEntrada(),
          completion.tokensSaida()));
      log.info(
          "ai.job.completed jobId={} sessaoId={} status=SUCESSO latencyMs={} tokensIn={} tokensOut={} provider={} template={} blueprint={} componentes={}",
          jobId,
          sessao.sessaoId(),
          conclusao.latenciaMs(),
          completion.tokensEntrada(),
          completion.tokensSaida(),
          llmProvider.id(),
          templateCodigo,
          blueprint == null ? null : blueprint.id(),
          pageSpec == null
              ? List.of()
              : pageSpec.blocos().stream().map(AiPageSpec.Bloco::componenteId).toList());
    } catch (AiJobCanceladoException ex) {
      log.info("ai.job.cancelled jobId={} sessaoId={}", jobId, sessao.sessaoId());
    } catch (Exception ex) {
      log.error("Falha no job AI {}", jobId, ex);
      lifecycleService.falhar(jobId, mensagemUsuario(ex), detalheTecnico(ex));
      log.info(
          "ai.job.completed jobId={} sessaoId={} status=ERRO provider={}",
          jobId,
          sessao.sessaoId(),
          llmProvider.id());
    }
  }

  private Map<String, String> coletarContexto(ContextoExecucao sessao) {
    Map<String, String> acumulado = new LinkedHashMap<>();
    for (AiMensagem mensagem : mensagemRepository.findBySessaoIdOrderByOrdemAsc(sessao.sessaoId())) {
      if (mensagem.getPapel() == AiPapelMensagem.USUARIO) {
        acumulado.putAll(AiPayloadJson.lerRespostas(objectMapper, mensagem.getPayloadJson()));
      }
      if (mensagem.getPapel() == AiPapelMensagem.ASSISTENTE) {
        try {
          if (mensagem.getPayloadJson() != null) {
            JsonNode node = objectMapper.readTree(mensagem.getPayloadJson()).path("contexto");
            if (node.isObject()) {
              node.fields().forEachRemaining(e -> {
                if (!e.getValue().asText("").isBlank()) {
                  acumulado.putIfAbsent(e.getKey(), e.getValue().asText());
                }
              });
            }
          }
        } catch (Exception ignored) {
          // ignore payload inválido
        }
      }
    }
    return acumulado;
  }

  private static List<PaginaBlocoResponse> componentesSelecionados(
      List<String> ids,
      List<PaginaBlocoResponse> catalogo) {
    Map<String, PaginaBlocoResponse> porId = catalogo.stream()
        .collect(java.util.stream.Collectors.toMap(PaginaBlocoResponse::id, bloco -> bloco));
    List<PaginaBlocoResponse> selecionados = ids.stream()
        .map(porId::get)
        .filter(java.util.Objects::nonNull)
        .toList();
    if (selecionados.size() != ids.size()) {
      throw new IllegalStateException(
          "Um componente aprovado não está mais disponível no catálogo DocFlow.");
    }
    return selecionados;
  }

  /**
   * Garante saída baseada no modelo da biblioteca. Se a IA inventar layout, usa o esqueleto.
   */
  static String decidirHtmlBiblioteca(String esqueleto, String htmlLlm) {
    if (esqueleto != null && !esqueleto.isBlank()) {
      if (AiTemplateSelector.preservaEstrutura(esqueleto, htmlLlm)) {
        return htmlLlm;
      }
      log.info("ai.job.html_fallback motivo=estrutura_divergente_do_modelo");
      return esqueleto;
    }
    return htmlLlm;
  }

  private JsonNode extrairJson(String raw) throws Exception {
    String text = raw == null ? "" : raw.trim();
    int start = text.indexOf('{');
    int end = text.lastIndexOf('}');
    if (start >= 0 && end > start) {
      text = text.substring(start, end + 1);
    }
    return objectMapper.readTree(text);
  }

  private static String mensagemUsuario(Exception ex) {
    String msg = ex.getMessage() == null ? "" : ex.getMessage();
    String lower = msg.toLowerCase(Locale.ROOT);
    if (lower.contains("json") || lower.contains("parse") || lower.contains("sem content")
        || lower.contains("conteudohtml")) {
      return "Resposta da IA inválida. Tente regenerar o rascunho.";
    }
    if (lower.contains("http") || lower.contains("falha ao chamar") || lower.contains("timeout")) {
      return "Falha ao consultar o provedor de IA. Tente novamente em instantes.";
    }
    return "Não foi possível gerar o rascunho. Tente novamente.";
  }

  private static String detalheTecnico(Exception ex) {
    StringWriter buffer = new StringWriter();
    ex.printStackTrace(new PrintWriter(buffer));
    return truncar(buffer.toString(), 8_000);
  }

  private static String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.asText().isBlank()) {
      return fallback;
    }
    return value.asText().trim();
  }

  private static String primeiroNaoVazio(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private static String extrairTituloBriefing(String briefing) {
    if (briefing == null || briefing.isBlank()) {
      return null;
    }
    return briefing.lines()
        .map(String::trim)
        .filter(linha -> !linha.isBlank())
        .findFirst()
        .map(linha -> linha.replaceFirst("^#{1,6}\\s+", "").trim())
        .filter(linha -> linha.length() <= 120)
        .orElse(null);
  }

  private static String slugify(String value) {
    String slug = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "pagina-ai" : slug;
  }

  private static String normalizarCodigoTela(String value) {
    String codigo = value == null ? "" : value.toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9_-]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return codigo.isBlank() ? "AI-DEMO" : codigo;
  }

  private static String truncar(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
