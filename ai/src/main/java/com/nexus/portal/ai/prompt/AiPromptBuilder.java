package com.nexus.portal.ai.prompt;

import com.nexus.portal.ai.prompt.AiPromptCatalogo.Prompt;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta os prompts do Nexus AI a partir de {@code resources/prompts/*.md} ({@link AiPromptCatalogo}).
 * O texto vive nos arquivos; aqui só se formata o contexto que preenche as variáveis.
 */
public final class AiPromptBuilder {

  private AiPromptBuilder() {}

  /** Par system/user pronto para o provider, com a versão que fica registrada na proposta. */
  public record PromptMontado(String system, String user, String versao) {

    static PromptMontado de(String nome, Map<String, String> variaveis) {
      Prompt system = AiPromptCatalogo.carregar(nome + ".system");
      Prompt user = AiPromptCatalogo.carregar(nome + ".user");
      return new PromptMontado(
          system.renderizar(Map.of()),
          user.renderizar(variaveis),
          nome + "@" + system.versao() + "." + user.versao());
    }
  }

  public static PromptMontado gerarPageSpec(
      String titulo,
      String codigoTela,
      String resumo,
      String briefing,
      Map<String, String> contexto,
      String templateCodigo,
      String templateNome,
      PaginaBlueprintResponse blueprint,
      List<PaginaBlocoResponse> componentes,
      List<String> instrucoes,
      String pageSpecAnterior) {
    Map<String, String> variaveis = new LinkedHashMap<>();
    variaveis.put("templateCodigo", nulo(templateCodigo));
    variaveis.put("templateNome", nulo(templateNome));
    variaveis.put("titulo", nulo(titulo));
    variaveis.put("codigoTela", nulo(codigoTela));
    variaveis.put("resumo", nulo(resumo));
    variaveis.put("briefing", nulo(briefing));
    variaveis.put("contexto", contexto == null ? "{}" : contexto.toString());
    variaveis.put("blueprint", descreverBlueprint(blueprint));
    variaveis.put("catalogo", descreverComponentes(componentes));
    variaveis.put("ajustes", descreverAjustes(instrucoes, pageSpecAnterior));
    return PromptMontado.de("gerar-page-spec", variaveis);
  }

  /** Organização de documento importado (páginas + sugestões). */
  public static PromptMontado analiseDocumento(String arquivo, String projetoNome, String manifestoJson) {
    return PromptMontado.de(
        "analise-documento",
        Map.of("arquivo", nulo(arquivo), "projetoNome", nulo(projetoNome), "manifesto", nulo(manifestoJson)));
  }

  /** Documento grande: só nomes de projeto e módulos; as páginas não vão ao modelo. */
  public static PromptMontado analiseDocumentoAmplo(String arquivo, String projetoNome, String manifestoJson) {
    return PromptMontado.de(
        "analise-documento-amplo",
        Map.of("arquivo", nulo(arquivo), "projetoNome", nulo(projetoNome), "manifesto", nulo(manifestoJson)));
  }

  /**
   * Ajuste de página existente (Fase B). {@code esboco} vem de {@code AiPaginaEsboco#descrever};
   * {@code patchAnterior} só entra no refinamento, para o modelo partir da proposta anterior.
   */
  public static PromptMontado ajustarPagina(
      String titulo,
      String resumo,
      String escopo,
      String esboco,
      List<PaginaBlocoResponse> catalogo,
      List<String> pedidos,
      String patchAnterior) {
    Map<String, String> variaveis = new LinkedHashMap<>();
    variaveis.put("titulo", nulo(titulo));
    variaveis.put("resumo", nulo(resumo));
    variaveis.put("escopo", nulo(escopo));
    variaveis.put("pedidos", pedidos == null ? "" : pedidos.stream()
        .map(pedido -> "- " + pedido.replaceAll("\\s+", " ").trim())
        .reduce((a, b) -> a + "\n" + b)
        .orElse(""));
    variaveis.put("esboco", nulo(esboco));
    variaveis.put("catalogo", descreverComponentes(catalogo));
    variaveis.put("propostaAnterior", patchAnterior == null || patchAnterior.isBlank() || pedidos == null
        || pedidos.size() < 2
        ? ""
        : "\nproposta anterior (refine a partir dela; mantenha o que não foi pedido para mudar):\n"
            + patchAnterior + "\n");
    return PromptMontado.de("ajustar-pagina", variaveis);
  }

  /**
   * Regeneração guiada: parte da versão anterior e aplica os pedidos do autor. O mais recente
   * prevalece em caso de conflito; os fatos continuam vindo do briefing.
   */
  static String descreverAjustes(List<String> instrucoes, String pageSpecAnterior) {
    if (instrucoes == null || instrucoes.isEmpty()) {
      return "";
    }
    String pedidos = instrucoes.stream()
        .map(instrucao -> "- " + instrucao.replaceAll("\\s+", " ").trim())
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
    String anterior = pageSpecAnterior == null || pageSpecAnterior.isBlank()
        ? ""
        : "\nversão anterior (PageSpec) — mantenha o que não foi pedido para mudar:\n"
            + pageSpecAnterior + "\n";
    return """

        ajustes pedidos pelo autor (em ordem; o último prevalece em caso de conflito):
        %s
        %s""".formatted(pedidos, anterior);
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

  private static String descreverBlueprint(PaginaBlueprintResponse blueprint) {
    if (blueprint == null) {
      return "Não identificado; use a melhor sequência editorial entre os componentes permitidos.";
    }
    String secoes = blueprint.secoes().stream()
        .map(secao -> secao.slot() + "=" + secao.componenteId()
            + " (" + secao.necessidade() + ")")
        .reduce((a, b) -> a + " -> " + b)
        .orElse("");
    return blueprint.id() + " v" + blueprint.versao() + " | " + blueprint.descricao()
        + " | ordem: " + secoes;
  }

  private static String truncar(String value, int max) {
    if (value == null) {
      return "";
    }
    String limpo = value.replaceAll("\\s+", " ").trim();
    return limpo.length() <= max ? limpo : limpo.substring(0, max) + "…";
  }
}
