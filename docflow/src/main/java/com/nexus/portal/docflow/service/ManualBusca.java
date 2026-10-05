package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.service.ManualCorpusService.Corpus;
import com.nexus.portal.docflow.service.ManualCorpusService.Secao;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recuperação determinística sobre o snapshot publicado (Onda E): código de tela exato + BM25 nas
 * seções, com o título da tela e da seção valendo mais. Sem embeddings — o corpus de um manual
 * cabe em memória e o resultado é explicável.
 *
 * <p>Guardrail (INT-502): o melhor trecho precisa conter ao menos metade dos termos da pergunta;
 * abaixo disso a resposta é "não sei", sem chamar a IA.
 *
 * <p>Sinônimos do cliente: cada troca ("nf" → "nota fiscal") é uma leitura alternativa da
 * pergunta; vale a leitura que encontra, com o melhor trecho.
 */
public final class ManualBusca {

  /** Fração mínima dos termos da pergunta presentes no melhor trecho. */
  static final double COBERTURA_MINIMA = 0.5;
  private static final double K1 = 1.2;
  private static final double B = 0.75;
  private static final int PESO_TITULO = 3;
  private static final Pattern CODIGO = Pattern.compile("\\b([A-Za-z][A-Za-z0-9]{1,15}(?:[-_][A-Za-z0-9]{1,15})+)\\b");
  private static final Set<String> VAZIAS = Set.of(
      "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das", "em", "no", "na",
      "nos", "nas", "por", "para", "pra", "com", "sem", "e", "ou", "que", "se", "como", "qual", "quais",
      "onde", "quando", "porque", "eu", "meu", "minha", "voce", "ao", "aos", "sao", "ser", "esta", "isso",
      "este", "esse", "essa", "tela", "pagina", "faco", "fazer", "consigo", "posso", "pode", "devo",
      "the", "how", "to", "is", "nao", "mais", "muito", "sobre", "ja", "tem", "ter", "vou");

  private ManualBusca() {}

  public record Resultado(Secao secao, double score, double cobertura) {}

  public record Resposta(List<Resultado> resultados, boolean encontrou, List<String> termos) {}

  public static Resposta buscar(Corpus corpus, String consulta, int limite) {
    List<String> alternativas = ManualSinonimos.alternativas(consulta, corpus.sinonimos());
    Resposta melhor = buscarLeitura(corpus, consulta, limite);
    for (String alternativa : alternativas.subList(1, alternativas.size())) {
      Resposta resposta = buscarLeitura(corpus, alternativa, limite);
      if (melhorQue(resposta, melhor)) {
        melhor = resposta;
      }
    }
    return melhor;
  }

  /** Encontrar vence não encontrar; entre iguais, o maior score do primeiro trecho. */
  private static boolean melhorQue(Resposta candidata, Resposta atual) {
    if (candidata.encontrou() != atual.encontrou()) {
      return candidata.encontrou();
    }
    return !candidata.resultados().isEmpty() && (atual.resultados().isEmpty()
        || candidata.resultados().getFirst().score() > atual.resultados().getFirst().score());
  }

  private static Resposta buscarLeitura(Corpus corpus, String consulta, int limite) {
    List<String> termos = termos(consulta);
    Set<String> codigos = codigosCitados(consulta, corpus);
    if (termos.isEmpty() && codigos.isEmpty()) {
      return new Resposta(List.of(), false, termos);
    }

    List<List<String>> docs = corpus.secoes().stream().map(ManualBusca::tokensDaSecao).toList();
    double media = docs.stream().mapToInt(List::size).average().orElse(1);
    Map<String, Integer> df = new HashMap<>();
    docs.forEach(tokens -> new LinkedHashSet<>(tokens).forEach(t -> df.merge(t, 1, Integer::sum)));
    int n = docs.size();

    List<Resultado> resultados = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Secao secao = corpus.secoes().get(i);
      List<String> tokens = docs.get(i);
      Map<String, Integer> tf = new HashMap<>();
      tokens.forEach(t -> tf.merge(t, 1, Integer::sum));
      double score = 0;
      int presentes = 0;
      for (String termo : new LinkedHashSet<>(termos)) {
        int f = tf.getOrDefault(termo, 0);
        if (f == 0) {
          continue;
        }
        presentes++;
        double idf = Math.log(1 + (n - df.get(termo) + 0.5) / (df.get(termo) + 0.5));
        score += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * tokens.size() / media));
      }
      boolean telaCitada = codigos.contains(secao.documento().codigoTela());
      if (telaCitada) {
        score += 10;
      }
      double cobertura = termos.isEmpty() ? 1 : (double) presentes / new LinkedHashSet<>(termos).size();
      if (score > 0) {
        resultados.add(new Resultado(secao, score, telaCitada ? Math.max(cobertura, 1) : cobertura));
      }
    }
    resultados.sort(Comparator.comparingDouble(Resultado::score).reversed());
    List<Resultado> melhores = resultados.stream().limit(limite).toList();
    boolean encontrou = !melhores.isEmpty() && melhores.getFirst().cobertura() >= COBERTURA_MINIMA;
    return new Resposta(melhores, encontrou, termos);
  }

  /** Códigos de tela do corpus citados na pergunta (sem diferenciar caixa). */
  static Set<String> codigosCitados(String consulta, Corpus corpus) {
    Set<String> codigos = new LinkedHashSet<>();
    Map<String, String> porMaiusculo = new HashMap<>();
    corpus.documentos().keySet().forEach(c -> porMaiusculo.put(c.toUpperCase(Locale.ROOT), c));
    Matcher m = CODIGO.matcher(consulta == null ? "" : consulta);
    while (m.find()) {
      String codigo = porMaiusculo.get(m.group(1).toUpperCase(Locale.ROOT));
      if (codigo != null) {
        codigos.add(codigo);
      }
    }
    return codigos;
  }

  private static List<String> tokensDaSecao(Secao secao) {
    List<String> tokens = new ArrayList<>();
    String titulos = secao.documento().titulo() + " " + (secao.titulo() == null ? "" : secao.titulo());
    for (int i = 0; i < PESO_TITULO; i++) {
      tokens.addAll(termos(titulos));
    }
    tokens.addAll(termos(secao.texto()));
    return tokens;
  }

  /** Minúsculas, sem acento, sem palavras vazias; plural simples vira singular. */
  static List<String> termos(String texto) {
    if (texto == null || texto.isBlank()) {
      return List.of();
    }
    String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    List<String> termos = new ArrayList<>();
    for (String bruto : normalizado.split("[^a-z0-9]+")) {
      if (bruto.length() < 2 || VAZIAS.contains(bruto)) {
        continue;
      }
      termos.add(radical(bruto));
    }
    return termos;
  }

  /** Sufixos do português, do mais longo ao mais curto ("exportação"/"exportar" → "export"). */
  private static final List<String> SUFIXOS = List.of(
      "acoes", "acao", "mente", "ancia", "encia", "ando", "endo", "indo", "ados", "adas", "idos", "idas",
      "ado", "ada", "ido", "ida", "ar", "er", "ir", "es", "os", "as", "s", "a", "e", "o");

  /**
   * Radical leve: corta o primeiro sufixo que deixe ao menos 3 letras. "filtrar", "filtro" e
   * "filtros" viram "filtr". Erra para mais (junta palavras próximas), o que a busca tolera.
   */
  static String radical(String termo) {
    if (termo.chars().anyMatch(Character::isDigit)) {
      return termo;
    }
    for (String sufixo : SUFIXOS) {
      if (termo.endsWith(sufixo) && termo.length() - sufixo.length() >= 3) {
        return termo.substring(0, termo.length() - sufixo.length());
      }
    }
    return termo;
  }
}
