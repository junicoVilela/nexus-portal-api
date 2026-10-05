package com.nexus.portal.docflow.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Expansão da consulta por sinônimos. A mesma regra roda no {@code app.js} do pacote: texto sem
 * acento, minúsculo, pontuação vira espaço; um termo do grupo só casa como palavra (ou frase)
 * inteira — "nf" não casa dentro de "info".
 */
public final class ManualSinonimos {

  /** Teto de leituras alternativas da mesma consulta. */
  static final int MAX_ALTERNATIVAS = 8;

  private ManualSinonimos() {}

  /** "NF-e" → "nf e"; "Emissão" → "emissao". */
  public static String normalizar(String texto) {
    if (texto == null) {
      return "";
    }
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .strip();
  }

  /**
   * A consulta original e, para cada termo de um grupo presente nela, a consulta com o termo
   * trocado pelos outros do grupo. "como emitir nf" + [nota fiscal, nf] → também "como emitir
   * nota fiscal".
   */
  public static List<String> alternativas(String consulta, List<List<String>> grupos) {
    Set<String> alternativas = new LinkedHashSet<>();
    alternativas.add(consulta == null ? "" : consulta);
    String normalizada = " " + normalizar(consulta) + " ";
    for (List<String> grupo : grupos == null ? List.<List<String>>of() : grupos) {
      for (String termo : grupo) {
        String alvo = " " + termo + " ";
        if (termo.isBlank() || !normalizada.contains(alvo)) {
          continue;
        }
        for (String outro : grupo) {
          if (!outro.equals(termo) && alternativas.size() < MAX_ALTERNATIVAS) {
            alternativas.add(normalizada.replace(alvo, " " + outro + " ").strip());
          }
        }
      }
    }
    return new ArrayList<>(alternativas);
  }
}
