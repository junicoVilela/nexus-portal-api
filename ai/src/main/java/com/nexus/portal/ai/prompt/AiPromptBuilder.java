package com.nexus.portal.ai.prompt;

import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import java.util.List;
import java.util.Map;

/**
 * Prompts do DocFlow AI — preenche modelos da biblioteca, não inventa layout.
 *
 * @see application/src/main/resources/db/migration/V10__docflow__04_seed_templates.sql
 */
public final class AiPromptBuilder {

  private AiPromptBuilder() {}

  public static String systemGerarPageSpec() {
    return """
        Você é redator técnico do Nexus DocFlow e planeja páginas usando exclusivamente o catálogo
        de componentes fornecido. Escreva em português do Brasil e em segunda pessoa quando orientar.

        Sua saída é uma PageSpec JSON; você NÃO escreve HTML, CSS, JavaScript, Markdown ou URLs.
        Cada item de blocos deve conter:
        - componenteId: um ID exato do catálogo;
        - textos: lista de objetos {slotId, valor}, usando somente os slots daquele componente.

        Regras:
        - escolha apenas componentes úteis ao objetivo e mantenha uma sequência editorial coerente;
        - use de 3 a 10 componentes, sem repetir componenteId;
        - preserve fatos do briefing e não invente permissões, regras, caminhos ou dados sensíveis;
        - não preencha slots puramente decorativos se o briefing não trouxer informação;
        - titulo, slug, codigoTela e resumo são obrigatórios;
        - responda somente com o JSON compatível com o schema solicitado.
        """;
  }

  public static String userGerarPageSpec(
      String titulo,
      String codigoTela,
      String resumo,
      String briefing,
      Map<String, String> contexto,
      String templateCodigo,
      String templateNome,
      List<PaginaBlocoResponse> componentes) {
    return """
        TAREFA=GERAR_PAGE_SPEC
        templateCodigo: %s
        templateNome: %s
        tituloSugerido: %s
        codigoTelaSugerido: %s
        resumoSugerido: %s

        briefing:
        %s

        contexto confirmado:
        %s

        catálogo permitido (ID, finalidade e slots editáveis):
        %s

        Monte a PageSpec com conteúdo específico para o briefing. Não retorne conteudoHtml.
        """.formatted(
        nulo(templateCodigo),
        nulo(templateNome),
        nulo(titulo),
        nulo(codigoTela),
        nulo(resumo),
        nulo(briefing),
        contexto == null ? "{}" : contexto,
        descreverComponentes(componentes));
  }

  public static String systemGerarRascunho() {
    return """
        Você é redator técnico do Nexus DocFlow. Gera rascunhos de manuais em português do Brasil
        PREENCHENDO um modelo HTML da biblioteca DocFlow — nunca inventa layout próprio.

        Responda APENAS um JSON válido com as chaves:
        titulo, slug, codigoTela, resumo, conteudoHtml.

        Regras obrigatórias de estrutura:
        - O campo conteudoHtml DEVE partir do esqueletoHtml fornecido (modelo da biblioteca).
        - Preserve a árvore DOM, tags, classes CSS e screen-placeholder do esqueleto.
        - Substitua apenas textos genéricos / placeholders pelo conteúdo do briefing.
        - NÃO invente seções novas, NÃO troque classes, NÃO remova screen-placeholder.
        - NÃO crie layout alternativo (mesmo que "mais bonito").
        - Se o esqueletoHtml estiver vazio (erro), use como último recurso as classes seed:
          doc-intro, doc-kicker, objective-card, doc-section, steps, checklist,
          result-card, screen-frame, screen-placeholder — preferindo modelos
          FUNCIONALIDADE, PASSO_A_PASSO, CONSULTA, LISTAR_REGISTROS.

        Regras de conteúdo:
        - Segunda pessoa do singular ("você") quando orientar o usuário.
        - HTML fragmento (sem html/head/body/script/style).
        - Não invente URLs de imagem nem links externos.
        - Não publique, não invente permissões RBAC, não inclua dados sensíveis.
        """;
  }

  public static String userGerarRascunho(
      String titulo,
      String codigoTela,
      String resumo,
      String briefing,
      Map<String, String> contexto,
      String esqueletoHtml,
      String templateCodigo,
      String templateNome) {
    return """
        TAREFA=GERAR_RASCUNHO
        modo=PREENCHER_MODELO_BIBLIOTECA
        templateCodigo: %s
        templateNome: %s
        titulo: %s
        codigoTela: %s
        resumo: %s

        briefing:
        %s

        contexto:
        %s

        esqueletoHtml:
        %s

        INSTRUCAO_FINAL: devolva conteudoHtml = esqueletoHtml acima com textos preenchidos; não invente outra estrutura.
        """.formatted(
        nulo(templateCodigo),
        nulo(templateNome),
        nulo(titulo),
        nulo(codigoTela),
        nulo(resumo),
        nulo(briefing),
        contexto == null ? "{}" : contexto.toString(),
        nulo(esqueletoHtml));
  }

  private static String nulo(String value) {
    return value == null ? "" : value;
  }

  private static String descreverComponentes(List<PaginaBlocoResponse> componentes) {
    if (componentes == null || componentes.isEmpty()) {
      return "[]";
    }
    return componentes.stream()
        .map(bloco -> {
          String slots = bloco.slots().stream()
              .map(slot -> slot.id() + " [" + slot.elemento() + "]: "
                  + truncar(slot.textoPadrao(), 100))
              .reduce((a, b) -> a + "; " + b)
              .orElse("");
          return "- " + bloco.id() + " | " + bloco.nome() + " | "
              + nulo(bloco.descricao()) + " | slots: " + slots;
        })
        .reduce((a, b) -> a + "\n" + b)
        .orElse("[]");
  }

  private static String truncar(String value, int max) {
    if (value == null) {
      return "";
    }
    String limpo = value.replaceAll("\\s+", " ").trim();
    return limpo.length() <= max ? limpo : limpo.substring(0, max) + "…";
  }
}
