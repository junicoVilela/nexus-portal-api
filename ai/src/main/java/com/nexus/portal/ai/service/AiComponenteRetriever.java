package com.nexus.portal.ai.service;

import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintSecaoResponse;
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

  private static final int LIMITE_SEM_BLUEPRINT = 7;
  private static final int LIMITE_ABSOLUTO = 12;
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
  private static final List<String> PADRAO = List.of(
      "introducao", "objetivo", "visao-tela", "passo-a-passo", "resultado-esperado");

  public List<PaginaBlocoResponse> recuperar(
      String templateCodigo, String briefing, List<PaginaBlocoResponse> catalogo) {
    return recuperar(templateCodigo, briefing, catalogo, null);
  }

  public List<PaginaBlocoResponse> recuperar(
      String templateCodigo,
      String briefing,
      List<PaginaBlocoResponse> catalogo,
      PaginaBlueprintResponse blueprint) {
    if (catalogo == null || catalogo.isEmpty()) {
      return List.of();
    }
    Map<String, PaginaBlocoResponse> porId = catalogo.stream()
        .filter(AiComponenteRetriever::elegivel)
        .collect(java.util.stream.Collectors.toMap(
            PaginaBlocoResponse::id, bloco -> bloco, (a, b) -> a));

    String texto = normalizar(briefing);
    if (blueprint != null) {
      return recuperarPeloBlueprint(texto, porId, blueprint);
    }

    Set<String> selecionados = new LinkedHashSet<>();
    PADRAO.forEach(id -> adicionarSeExiste(selecionados, porId, id));
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
          if (selecionados.size() < LIMITE_SEM_BLUEPRINT) {
            selecionados.add(id);
          }
        });

    return selecionados.stream().limit(LIMITE_SEM_BLUEPRINT).map(porId::get).toList();
  }

  private static List<PaginaBlocoResponse> recuperarPeloBlueprint(
      String briefing,
      Map<String, PaginaBlocoResponse> catalogo,
      PaginaBlueprintResponse blueprint) {
    int limite = Math.min(LIMITE_ABSOLUTO, blueprint.maximoComponentes());
    Set<String> selecionados = new LinkedHashSet<>();
    List<PaginaBlueprintSecaoResponse> opcionais = new ArrayList<>();

    for (PaginaBlueprintSecaoResponse secao : blueprint.secoes()) {
      if ("OPCIONAL".equals(secao.necessidade())) {
        opcionais.add(secao);
        continue;
      }
      if (selecionados.size() < limite) {
        melhorComponente(secao, briefing, catalogo)
            .ifPresent(bloco -> selecionados.add(bloco.id()));
      }
    }

    opcionais.stream()
        .map(secao -> melhorComponente(secao, briefing, catalogo).orElse(null))
        .filter(java.util.Objects::nonNull)
        .map(bloco -> new Pontuado(bloco, pontuar(briefing, bloco)))
        .filter(item -> item.pontos() > 0)
        .sorted(Comparator.comparingInt(Pontuado::pontos).reversed()
            .thenComparing(item -> item.bloco().id()))
        .forEach(item -> {
          if (selecionados.size() < limite) {
            selecionados.add(item.bloco().id());
          }
        });

    if (selecionados.size() < blueprint.minimoComponentes()) {
      opcionais.stream()
          .map(secao -> melhorComponente(secao, briefing, catalogo).orElse(null))
          .filter(java.util.Objects::nonNull)
          .map(PaginaBlocoResponse::id)
          .forEach(id -> {
            if (selecionados.size() < blueprint.minimoComponentes()
                && selecionados.size() < limite) {
              selecionados.add(id);
            }
          });
    }

    catalogo.values().stream()
        .filter(bloco -> !selecionados.contains(bloco.id()))
        .map(bloco -> new Pontuado(bloco, pontuar(briefing, bloco)))
        .filter(item -> item.pontos() > 0)
        .sorted(Comparator.comparingInt(Pontuado::pontos).reversed()
            .thenComparing(item -> item.bloco().id()))
        .forEach(item -> {
          if (selecionados.size() < limite) {
            selecionados.add(item.bloco().id());
          }
        });

    return selecionados.stream().limit(limite).map(catalogo::get).toList();
  }

  private static java.util.Optional<PaginaBlocoResponse> melhorComponente(
      PaginaBlueprintSecaoResponse secao,
      String briefing,
      Map<String, PaginaBlocoResponse> catalogo) {
    List<String> ids = new ArrayList<>();
    ids.add(secao.componenteId());
    ids.addAll(secao.alternativas());
    return ids.stream()
        .map(catalogo::get)
        .filter(java.util.Objects::nonNull)
        .max(Comparator.comparingInt((PaginaBlocoResponse bloco) -> pontuar(briefing, bloco))
            .thenComparingInt(bloco -> -ids.indexOf(bloco.id())));
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

  private static String normalizar(String value) {
    String semAcentos = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  private record Pontuado(PaginaBlocoResponse bloco, int pontos) {
  }
}
