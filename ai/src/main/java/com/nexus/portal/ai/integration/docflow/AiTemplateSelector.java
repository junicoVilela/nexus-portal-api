package com.nexus.portal.ai.integration.docflow;

import com.nexus.portal.docflow.entity.PaginaTemplate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Identifica o modelo da biblioteca DocFlow que melhor se encaixa no briefing.
 * Não força {@code FUNCIONALIDADE} quando outro template pontua melhor.
 */
public final class AiTemplateSelector {

  public static final double CONFIANCA_AUTO_SELECAO = 0.70;

  private static final Map<String, Pattern> CODIGO_POR_PADRAO = new LinkedHashMap<>();

  static {
    CODIGO_POR_PADRAO.put(
        "LISTAR_REGISTROS",
        Pattern.compile(
            "\\b(listar|listagem|grade|grid|tabela|exportar|resultado[s]?)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "CONSULTA",
        Pattern.compile(
            "\\b(consulta|consultar|filtro|filtrar|pesquisa|pesquisar|buscar|busca)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "INCLUIR_REGISTRO",
        Pattern.compile(
            "\\b(incluir|inclus[aã]o|cadastrar|cadastro|novo registro|criar registro)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "EDITAR_REGISTRO",
        Pattern.compile(
            "\\b(editar|edi[cç][aã]o|alterar|atualizar registro)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "CADASTRO",
        Pattern.compile(
            "\\b(formul[aá]rio|campos obrigat|valida[cç][aã]o)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "PASSO_A_PASSO",
        Pattern.compile(
            "\\b(passo\\s*a\\s*passo|procedimento|tutorial|como fazer|fluxo operacional)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "FAQ",
        Pattern.compile(
            "\\b(faq|d[uú]vidas?|perguntas frequentes)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "SOLUCAO_PROBLEMAS",
        Pattern.compile(
            "\\b(erro|problema|falha|troubleshooting|solu[cç][aã]o de problemas)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "RELATORIO",
        Pattern.compile(
            "\\b(relat[oó]rio|dashboard|indicador|m[eé]trica)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "DICIONARIO_CAMPOS",
        Pattern.compile(
            "\\b(dicion[aá]rio|gloss[aá]rio|campos da tela)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "PRIMEIROS_PASSOS",
        Pattern.compile(
            "\\b(primeiros passos|getting started|onboarding|come[cç]ar)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    CODIGO_POR_PADRAO.put(
        "PROCESSO",
        Pattern.compile(
            "\\b(processo|workflow|fluxo de trabalho)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
  }

  private AiTemplateSelector() {}

  public static Optional<PaginaTemplate> selecionar(
      String briefing, List<PaginaTemplate> biblioteca) {
    return recomendar(briefing, biblioteca).stream()
        .findFirst()
        .map(Recomendacao::template);
  }

  /**
   * Retorna o ranking explicável usado tanto pela API quanto pelo worker.
   *
   * <p>A confiança mede a força dos sinais locais e a distância para o segundo colocado. Ela não
   * pretende ser uma probabilidade estatística; abaixo do limiar a interface deve pedir confirmação.
   */
  public static List<Recomendacao> recomendar(
      String briefing, List<PaginaTemplate> biblioteca) {
    if (biblioteca == null || biblioteca.isEmpty()) {
      return List.of();
    }
    String texto = briefing == null ? "" : briefing.toLowerCase(Locale.ROOT);

    List<Score> ranking = new ArrayList<>();
    for (PaginaTemplate template : biblioteca) {
      ranking.add(new Score(template, pontuar(texto, template)));
    }
    ranking.sort(Comparator
        .comparingInt(Score::pontos)
        .reversed()
        .thenComparing(score -> normalizar(score.template().getNome())));

    if (ranking.get(0).pontos() == 0) {
      PaginaTemplate fallback = porCodigo(biblioteca, DocFlowAiBridge.TEMPLATE_PADRAO)
          .orElse(biblioteca.get(0));
      ranking.removeIf(score -> score.template() == fallback);
      ranking.add(0, new Score(fallback, 0));
    }

    int melhor = ranking.get(0).pontos();
    int segundo = ranking.size() > 1 ? ranking.get(1).pontos() : 0;
    List<Recomendacao> recomendacoes = new ArrayList<>();
    for (int indice = 0; indice < Math.min(3, ranking.size()); indice++) {
      Score score = ranking.get(indice);
      double confianca = confianca(score.pontos(), indice == 0 ? segundo : melhor);
      recomendacoes.add(new Recomendacao(
          score.template(),
          confianca,
          motivo(score.template(), score.pontos(), indice == 0)));
    }
    return List.copyOf(recomendacoes);
  }

  static int pontuar(String briefingLower, PaginaTemplate template) {
    if (template == null) {
      return 0;
    }
    int pontos = 0;
    String codigo = template.getCodigo() == null ? "" : template.getCodigo();
    int operacoesCrud = contarOperacoesCrud(briefingLower);

    Pattern padraoCodigo = CODIGO_POR_PADRAO.get(codigo.toUpperCase(Locale.ROOT));
    if (padraoCodigo != null && padraoCodigo.matcher(briefingLower).find()) {
      pontos += 100;
    }
    if (operacoesCrud >= 3 && "CADASTRO".equalsIgnoreCase(codigo)) {
      pontos += 180;
    } else if (operacoesCrud >= 3 && "FUNCIONALIDADE".equalsIgnoreCase(codigo)) {
      pontos += 130;
    }
    if (operacoesCrud == 1
        && "FUNCIONALIDADE".equalsIgnoreCase(codigo)
        && Pattern.compile("\\b(excluir|exclus[aã]o|remover)\\b").matcher(briefingLower).find()) {
      pontos += 100;
    }

    // Overlap com metadados do próprio modelo (inclui personalizados da biblioteca).
    pontos += overlapTokens(briefingLower, normalizar(codigo));
    pontos += overlapTokens(briefingLower, normalizar(template.getNome())) * 2;
    pontos += overlapTokens(briefingLower, normalizar(template.getDescricao()));

    // Evita empurrar FUNCIONALIDADE só porque o nome aparece em textos genéricos.
    if ("FUNCIONALIDADE".equalsIgnoreCase(codigo) && pontos < 40) {
      pontos = Math.max(0, pontos - 10);
    }
    return pontos;
  }

  private static int contarOperacoesCrud(String texto) {
    int operacoes = 0;
    for (Pattern padrao : List.of(
        Pattern.compile("\\b(consultar|consulta|listar|listagem|pesquisar)\\b"),
        Pattern.compile("\\b(cadastrar|cadastro|incluir|inclus[aã]o|novo registro)\\b"),
        Pattern.compile("\\b(editar|edi[cç][aã]o|alterar|atualizar)\\b"),
        Pattern.compile("\\b(excluir|exclus[aã]o|remover)\\b"))) {
      if (padrao.matcher(texto).find()) {
        operacoes++;
      }
    }
    return operacoes;
  }

  private static int overlapTokens(String briefingLower, String campo) {
    if (campo.isBlank() || briefingLower.isBlank()) {
      return 0;
    }
    int hits = 0;
    for (String token : campo.split("[\\s_\\-/]+")) {
      if (token.length() < 4) {
        continue;
      }
      if (briefingLower.contains(token)) {
        hits += 3;
      }
    }
    return hits;
  }

  private static String normalizar(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private static double confianca(int pontos, int concorrente) {
    if (pontos <= 0) {
      return 0.35;
    }
    double base = pontos >= 100 ? 0.82 : pontos >= 30 ? 0.68 : pontos >= 9 ? 0.56 : 0.44;
    if (concorrente > 0 && concorrente >= pontos * 0.90) {
      base -= 0.20;
    }
    double separacao = Math.min(0.13, Math.max(0, pontos - concorrente) / 100.0);
    return Math.round(Math.min(0.95, base + separacao) * 100.0) / 100.0;
  }

  private static String motivo(PaginaTemplate template, int pontos, boolean principal) {
    if (pontos <= 0) {
      return "Modelo padrão da biblioteca; o texto ainda não contém sinais suficientes.";
    }
    if (pontos >= 100) {
      return "O objetivo e os termos do texto correspondem ao tipo " + template.getNome() + ".";
    }
    return (principal ? "Melhor correspondência" : "Alternativa compatível")
        + " pelos metadados do modelo e pelo conteúdo informado.";
  }

  public static Optional<PaginaTemplate> porCodigo(List<PaginaTemplate> biblioteca, String codigo) {
    if (codigo == null || biblioteca == null) {
      return Optional.empty();
    }
    return biblioteca.stream()
        .filter(t -> codigo.equalsIgnoreCase(t.getCodigo()))
        .findFirst();
  }

  private record Score(PaginaTemplate template, int pontos) {
  }

  public record Recomendacao(PaginaTemplate template, double confianca, String motivo) {

    public boolean permiteAutoSelecao() {
      return confianca >= CONFIANCA_AUTO_SELECAO;
    }
  }
}
