package com.nexus.portal.ai.service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;

/**
 * Quanto do texto proposto pela IA ainda está na página: fração das palavras da proposta que
 * continuam no conteúdo atual (contagem de palavras com repetição, sem acento e sem caixa).
 *
 * <p>É uma medida de "reescrita", não de ordem: 1.0 = o autor manteve todas as palavras; perto
 * de 0 = reescreveu quase tudo. Barata (linear) e insensível a mudanças de formatação HTML.
 */
public final class AiTextoMantido {

  private AiTextoMantido() {}

  /** Nulo quando a proposta não tem texto para comparar. */
  public static Double fracao(String htmlProposta, String htmlPagina) {
    Map<String, Integer> proposta = palavras(htmlProposta);
    int total = proposta.values().stream().mapToInt(Integer::intValue).sum();
    if (total == 0) {
      return null;
    }
    Map<String, Integer> pagina = palavras(htmlPagina);
    int mantidas = proposta.entrySet().stream()
        .mapToInt(e -> Math.min(e.getValue(), pagina.getOrDefault(e.getKey(), 0)))
        .sum();
    return (double) mantidas / total;
  }

  private static Map<String, Integer> palavras(String html) {
    Map<String, Integer> contagem = new HashMap<>();
    if (html == null || html.isBlank()) {
      return contagem;
    }
    String texto = Normalizer.normalize(Jsoup.parse(html).text(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    for (String palavra : texto.split("[^\\p{L}\\p{N}]+")) {
      if (palavra.length() > 1) {
        contagem.merge(palavra, 1, Integer::sum);
      }
    }
    return contagem;
  }
}
