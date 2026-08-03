package com.nexus.portal.ai.prompt;

import java.util.Map;

/**
 * Prompts do DocFlow AI (tom PT-BR, classes seed, placeholders de captura).
 *
 * @see application/src/main/resources/db/migration/V10__docflow__04_seed_templates.sql
 */
public final class AiPromptBuilder {

  private AiPromptBuilder() {}

  public static String systemGerarRascunho() {
    return """
        Você é redator técnico do Nexus DocFlow. Gera rascunhos de manuais de usuário em português do Brasil.

        Responda APENAS um JSON válido com as chaves:
        titulo, slug, codigoTela, resumo, conteudoHtml.

        Regras de conteúdo:
        - Segunda pessoa do singular ("você") quando orientar o usuário.
        - HTML fragmento (sem html/head/body/script/style).
        - Envolva o conteúdo útil em seções com classes do design system DocFlow:
          doc-intro, doc-kicker, objective-card, doc-section, steps, checklist,
          result-card, screen-frame, screen-placeholder.
        - Quando não houver captura real, use:
          <div class="screen-placeholder"><strong>Insira captura</strong><span>Substitua pela imagem real da tela.</span></div>
        - Não invente URLs de imagem nem links externos.
        - Prefira templates seed como referência de estrutura: FUNCIONALIDADE, PASSO_A_PASSO, CONSULTA.
        - Não publique, não invente permissões RBAC, não inclua dados sensíveis.
        - Se receber esqueletoHtml, preserve a estrutura e complete o texto; não remova screen-placeholder sem motivo.
        """;
  }

  public static String userGerarRascunho(
      String titulo,
      String codigoTela,
      String resumo,
      String briefing,
      Map<String, String> contexto,
      String esqueletoHtml) {
    return """
        TAREFA=GERAR_RASCUNHO
        titulo: %s
        codigoTela: %s
        resumo: %s

        briefing:
        %s

        contexto:
        %s

        esqueletoHtml:
        %s
        """.formatted(
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
}
