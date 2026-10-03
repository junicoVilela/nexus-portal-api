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
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.prompt.AiPromptBuilder;
import com.nexus.portal.ai.prompt.AiPromptBuilder.PromptMontado;
import com.nexus.portal.ai.provider.FakeLlmProvider;
import com.nexus.portal.ai.provider.LlmCompletion;
import com.nexus.portal.ai.provider.LlmProvider;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.service.AiJobLifecycleService.ContextoExecucao;
import com.nexus.portal.ai.service.AiJobLifecycleService.ResultadoConclusao;
import com.nexus.portal.ai.service.AiJobLifecycleService.ResultadoGeracao;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Executa o job {@code GERAR_RASCUNHO} em etapas, cada uma refletida no progresso via SSE:
 *
 * <ol>
 *   <li>{@link #prepararEntrada} — título, código e resumo a partir do briefing e das respostas;
 *   <li>{@link #selecionarEstrutura} — modelo, blueprint e componentes candidatos do catálogo;
 *   <li>{@link #gerarConteudo} — PageSpec pela IA, com fallback registrado em {@code avisos};
 *   <li>{@link #avaliarQualidade} — checklist do DocFlow sobre o HTML renderizado;
 *   <li>{@link #concluir} — persiste a proposta e registra métricas.
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobWorkerService {

  private static final String CODIGO_TELA_PADRAO = "AI-DEMO";

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
      Entrada entrada = prepararEntrada(sessao);
      lifecycleService.avancar(jobId, AiJobEtapa.SELECIONANDO_ESTRUTURA, 30);
      Estrutura estrutura = selecionarEstrutura(sessao);
      lifecycleService.avancar(jobId, AiJobEtapa.GERANDO_CONTEUDO, 50);
      Conteudo conteudo = gerarConteudo(jobId, sessao, entrada, estrutura);
      lifecycleService.avancar(jobId, AiJobEtapa.VALIDANDO_QUALIDADE, 80);
      String qualidadeJson = avaliarQualidade(conteudo);
      lifecycleService.avancar(jobId, AiJobEtapa.FINALIZANDO, 95);
      concluir(jobId, sessao, estrutura, conteudo, qualidadeJson);
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

  /** Respostas do autor prevalecem sobre o que a triagem extrai do texto. */
  private Entrada prepararEntrada(ContextoExecucao sessao) {
    Map<String, String> respostas = coletarContexto(sessao);
    Map<String, String> contexto = new LinkedHashMap<>(
        triagemService.avaliar(sessao.objetivo(), sessao.briefing(), respostas).contextoExtraido());
    contexto.putAll(respostas);
    return new Entrada(
        contexto,
        primeiroNaoVazio(contexto.get("titulo"), extrairTituloBriefing(sessao.briefing()), "Página gerada"),
        normalizarCodigoTela(primeiroNaoVazio(contexto.get("codigoTela"), CODIGO_TELA_PADRAO)),
        primeiroNaoVazio(contexto.get("resumo"), contexto.get("fluxo"), sessao.briefing()));
  }

  private Estrutura selecionarEstrutura(ContextoExecucao sessao) {
    PaginaTemplate template = docFlowAiBridge
        .buscarTemplate(sessao.templateId(), sessao.projetoId(), sessao.clienteId(), sessao.briefing())
        .orElse(null);
    String templateCodigo = template == null ? null : template.getCodigo();
    PaginaBlueprintResponse blueprint = docFlowAiBridge.buscarBlueprint(templateCodigo).orElse(null);
    List<PaginaBlocoResponse> catalogo = docFlowAiBridge.listarBlocos();
    List<PaginaBlocoResponse> candidatos = sessao.componentesSelecionados().isEmpty()
        ? componenteRetriever.recuperar(templateCodigo, sessao.briefing(), catalogo, blueprint)
        : componentesSelecionados(sessao.componentesSelecionados(), catalogo);
    if (candidatos.isEmpty()) {
      throw new IllegalStateException(
          "Nenhum componente do catálogo DocFlow disponível para montar a página.");
    }
    return new Estrutura(
        template == null ? null : template.getId(),
        template == null ? null : template.getVersaoAtual(),
        templateCodigo,
        template == null ? null : template.getNome(),
        blueprint,
        candidatos);
  }

  /**
   * Pede a PageSpec à IA e renderiza no servidor. Resposta inválida não derruba o job: cai no
   * fallback com textos padrão do catálogo e registra o motivo em {@code avisos}, que a UI destaca.
   */
  private Conteudo gerarConteudo(UUID jobId, ContextoExecucao sessao, Entrada entrada, Estrutura estrutura) {
    List<String> avisos = new ArrayList<>();
    if (FakeLlmProvider.ID.equals(llmProvider.id())) {
      avisos.add("Gerado pelo provider de demonstração (sem API key): o texto não reflete o briefing.");
    }
    List<PaginaBlocoResponse> candidatos = estrutura.candidatos();
    PromptMontado prompt = AiPromptBuilder.gerarPageSpec(
        entrada.titulo(),
        entrada.codigoTela(),
        truncar(entrada.resumoHint(), 500),
        sessao.briefing(),
        entrada.contexto(),
        estrutura.templateCodigo(),
        estrutura.templateNome(),
        estrutura.blueprint(),
        candidatos,
        coletarInstrucoes(sessao),
        truncar(sessao.pageSpecAnterior(), 12_000));
    LlmCompletion completion = llmProvider.completarEstruturado(
        prompt.system(), prompt.user(), pageSpecService.schema(candidatos));

    JsonNode json;
    try {
      json = extrairJson(completion.content());
    } catch (Exception ex) {
      log.warn("ai.job.pagespec_fallback jobId={} motivo=json_invalido detalhe={}", jobId, ex.getMessage());
      avisos.add("A IA devolveu uma resposta inválida; os blocos usam textos padrão do modelo.");
      json = objectMapper.createObjectNode()
          .put("titulo", entrada.titulo())
          .put("slug", slugify(entrada.titulo()))
          .put("codigoTela", entrada.codigoTela())
          .put("resumo", truncar(entrada.resumoHint(), 280));
    }
    AiPageSpec pageSpec;
    try {
      pageSpec = pageSpecService.interpretar(json, candidatos, estrutura.blueprint());
    } catch (IllegalArgumentException ex) {
      log.warn(
          "ai.job.pagespec_fallback jobId={} motivo={} componentes={}",
          jobId,
          ex.getMessage(),
          candidatos.stream().map(PaginaBlocoResponse::id).toList());
      avisos.add("A resposta da IA não seguiu a estrutura esperada; parte do conteúdo usa textos padrão.");
      pageSpec = pageSpecService.fallback(
          text(json, "titulo", entrada.titulo()),
          text(json, "slug", slugify(entrada.titulo())),
          text(json, "codigoTela", entrada.codigoTela()),
          text(json, "resumo", truncar(entrada.resumoHint(), 280)),
          candidatos,
          estrutura.blueprint());
    }
    if (!sessao.componentesSelecionados().isEmpty()) {
      pageSpec = pageSpecService.garantirComponentes(pageSpec, candidatos);
    }
    pageSpec = briefingPageSpecEnricher.enriquecer(pageSpec, candidatos, sessao.briefing());
    String html = htmlSanitizer.sanitizar(pageSpecService.renderizar(pageSpec));
    if (html == null || html.isBlank()) {
      throw new IllegalStateException("A PageSpec não produziu conteúdo utilizável.");
    }

    String codigoTela = truncar(normalizarCodigoTela(pageSpec.codigoTela()), 120);
    if (CODIGO_TELA_PADRAO.equals(codigoTela)) {
      avisos.add("Código de tela não identificado; ajuste o código antes de salvar a página.");
    }
    return new Conteudo(
        pageSpec,
        truncar(pageSpec.titulo(), 200),
        truncar(pageSpec.slug(), 200),
        codigoTela,
        truncar(pageSpec.resumo(), 2_000),
        html,
        avisos,
        completion,
        prompt.versao());
  }

  private String avaliarQualidade(Conteudo conteudo) throws Exception {
    ResultadoQualidade qualidade = docFlowAiBridge.avaliarQualidade(
        conteudo.titulo(), conteudo.codigoTela(), conteudo.resumo(), conteudo.html());
    return objectMapper.writeValueAsString(Map.of(
        "aptoParaRevisao", qualidade.aptoParaRevisao(),
        "itens", qualidade.itens().stream()
            .map(i -> new AiQualidadeItemResponse(
                i.codigo(), i.titulo(), i.descricao(), i.ok(), i.severidade().name()))
            .toList()));
  }

  private void concluir(
      UUID jobId,
      ContextoExecucao sessao,
      Estrutura estrutura,
      Conteudo conteudo,
      String qualidadeJson) throws Exception {
    AiPropostaTipo tipo = sessao.objetivo() == AiObjetivo.ATUALIZAR_PAGINA
        ? AiPropostaTipo.ATUALIZACAO
        : AiPropostaTipo.NOVA;
    LlmCompletion completion = conteudo.completion();
    ResultadoConclusao conclusao = lifecycleService.concluir(jobId, new ResultadoGeracao(
        tipo,
        conteudo.titulo(),
        conteudo.slug(),
        conteudo.codigoTela(),
        conteudo.resumo(),
        conteudo.html(),
        estrutura.templateId(),
        estrutura.templateVersao(),
        qualidadeJson,
        objectMapper.writeValueAsString(conteudo.pageSpec()),
        completion.tokensEntrada(),
        completion.tokensSaida(),
        conteudo.avisos(),
        conteudo.promptVersao()));
    log.info(
        "ai.job.completed jobId={} sessaoId={} status=SUCESSO latencyMs={} tokensIn={} tokensOut={} provider={} prompt={} template={} blueprint={} componentes={} avisos={}",
        jobId,
        sessao.sessaoId(),
        conclusao.latenciaMs(),
        completion.tokensEntrada(),
        completion.tokensSaida(),
        llmProvider.id(),
        conteudo.promptVersao(),
        estrutura.templateCodigo(),
        estrutura.blueprint() == null ? null : estrutura.blueprint().id(),
        conteudo.pageSpec().blocos().stream().map(AiPageSpec.Bloco::componenteId).toList(),
        conteudo.avisos().size());
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

  /** Pedidos de ajuste feitos ao regenerar, em ordem cronológica. */
  private List<String> coletarInstrucoes(ContextoExecucao sessao) {
    return mensagemRepository.findBySessaoIdOrderByOrdemAsc(sessao.sessaoId()).stream()
        .filter(mensagem -> mensagem.getPapel() == AiPapelMensagem.USUARIO)
        .map(mensagem -> AiPayloadJson.lerInstrucao(objectMapper, mensagem.getPayloadJson()))
        .filter(java.util.Objects::nonNull)
        .toList();
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
    return codigo.isBlank() ? CODIGO_TELA_PADRAO : codigo;
  }

  private static String truncar(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }

  private record Entrada(Map<String, String> contexto, String titulo, String codigoTela, String resumoHint) {}

  private record Estrutura(
      UUID templateId,
      Integer templateVersao,
      String templateCodigo,
      String templateNome,
      PaginaBlueprintResponse blueprint,
      List<PaginaBlocoResponse> candidatos) {}

  private record Conteudo(
      AiPageSpec pageSpec,
      String titulo,
      String slug,
      String codigoTela,
      String resumo,
      String html,
      List<String> avisos,
      LlmCompletion completion,
      String promptVersao) {}
}
