package com.nexus.portal.ai.service;

import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Recuperação híbrida local para componentes de página.
 *
 * <p>A taxonomia do template garante cobertura mínima e a pontuação lexical acrescenta componentes
 * relevantes ao briefing. O contrato permite trocar a pontuação por embeddings no futuro sem
 * alterar prompt, renderer ou frontend.
 */
@Service
public class AiComponenteRetriever {

  private static final int LIMITE = 10;
  private static final Set<String> TERMOS_GENERICOS = Set.of(
      "pagina",
      "tela",
      "usuario",
      "usuarios",
      "conteudo",
      "resultado",
      "sistema",
      "estrutura",
      "orientacao",
      "referencia",
      "disponivel",
      "disponiveis",
      "esperado",
      "descricao");
  private static final Map<String, List<String>> COMPONENTES_POR_TEMPLATE = Map.ofEntries(
      Map.entry("LISTAR_REGISTROS", List.of(
          "introducao", "objetivo", "pre-requisitos", "visao-tela", "filtros-resultado",
          "acoes-tela", "resultado-esperado")),
      Map.entry("CONSULTA", List.of(
          "introducao", "objetivo", "pre-requisitos", "visao-tela", "filtros-resultado",
          "acoes-tela", "resultado-esperado")),
      Map.entry("INCLUIR_REGISTRO", List.of(
          "introducao", "objetivo", "pre-requisitos", "visao-tela", "campos-criticos",
          "passo-a-passo", "regras", "resultado-esperado")),
      Map.entry("CADASTRO", List.of(
          "introducao", "objetivo", "pre-requisitos", "visao-tela", "campos-criticos",
          "passo-a-passo", "regras", "resultado-esperado")),
      Map.entry("EDITAR_REGISTRO", List.of(
          "introducao", "objetivo", "pre-requisitos", "visao-tela", "campos-criticos",
          "passo-a-passo", "regras", "resultado-esperado")),
      Map.entry("PASSO_A_PASSO", List.of(
          "introducao", "pre-requisitos", "visao-tela", "passo-a-passo",
          "checklist-validacao", "resultado-esperado")),
      Map.entry("FAQ", List.of("introducao", "faq", "links-relacionados")),
      Map.entry("SOLUCAO_PROBLEMAS", List.of(
          "introducao", "mensagens-sistema", "callout-erro", "checklist-validacao")),
      Map.entry("RELATORIO", List.of(
          "introducao", "objetivo", "visao-tela", "filtros-resultado", "acoes-tela",
          "resultado-esperado")),
      Map.entry("DICIONARIO_CAMPOS", List.of("introducao", "dicionario", "callout-info")),
      Map.entry("PROCESSO", List.of(
          "introducao", "objetivo", "fluxo", "regras", "resultado-esperado")),
      Map.entry("PRIMEIROS_PASSOS", List.of(
          "introducao", "jornada", "boas-praticas", "links-relacionados")));

  private static final List<String> PADRAO = List.of(
      "introducao", "objetivo", "visao-tela", "passo-a-passo", "resultado-esperado");

  public List<PaginaBlocoResponse> recuperar(
      String templateCodigo, String briefing, List<PaginaBlocoResponse> catalogo) {
    if (catalogo == null || catalogo.isEmpty()) {
      return List.of();
    }
    Map<String, PaginaBlocoResponse> porId = catalogo.stream()
        .filter(AiComponenteRetriever::elegivel)
        .collect(java.util.stream.Collectors.toMap(
            PaginaBlocoResponse::id, bloco -> bloco, (a, b) -> a));

    Set<String> selecionados = new LinkedHashSet<>();
    List<String> base = COMPONENTES_POR_TEMPLATE.getOrDefault(
        normalizarCodigo(templateCodigo), PADRAO);
    base.forEach(id -> adicionarSeExiste(selecionados, porId, id));

    String texto = normalizar(briefing);
    List<Pontuado> ranking = new ArrayList<>();
    for (PaginaBlocoResponse bloco : porId.values()) {
      ranking.add(new Pontuado(bloco, pontuar(texto, bloco)));
    }
    ranking.stream()
        .filter(item -> item.pontos() > 0)
        .sorted(Comparator.comparingInt(Pontuado::pontos).reversed()
            .thenComparing(item -> item.bloco().id()))
        .map(Pontuado::bloco)
        .map(PaginaBlocoResponse::id)
        .forEach(id -> {
          if (selecionados.size() < LIMITE) {
            selecionados.add(id);
          }
        });

    return selecionados.stream().limit(LIMITE).map(porId::get).toList();
  }

  private static boolean elegivel(PaginaBlocoResponse bloco) {
    return bloco != null
        && !"Kits".equalsIgnoreCase(bloco.categoria())
        && !bloco.id().startsWith("badge-");
  }

  private static int pontuar(String briefing, PaginaBlocoResponse bloco) {
    if (briefing.isBlank()) {
      return 0;
    }
    String metadados = normalizar(
        bloco.id() + " " + bloco.nome() + " " + bloco.descricao() + " " + bloco.categoria());
    int pontos = 0;
    for (String token : new LinkedHashSet<>(List.of(metadados.split("\\s+")))) {
      if (token.length() >= 4
          && !TERMOS_GENERICOS.contains(token)
          && briefing.matches(".*\\b" + java.util.regex.Pattern.quote(token) + "\\b.*")) {
        pontos += token.length() >= 8 ? 4 : 2;
      }
    }
    return pontos;
  }

  private static void adicionarSeExiste(
      Set<String> selecionados, Map<String, PaginaBlocoResponse> porId, String id) {
    if (porId.containsKey(id)) {
      selecionados.add(id);
    }
  }

  private static String normalizarCodigo(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private static String normalizar(String value) {
    String semAcentos = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  private record Pontuado(PaginaBlocoResponse bloco, int pontos) {
  }
}
