package com.nexus.portal.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Estrutura editorial extraída de um briefing em Markdown ou texto simples. */
record AiBriefingDocument(String titulo, List<Secao> secoes) {

  private static final Pattern TITULO_MARKDOWN = Pattern.compile("^\\s*(#{1,6})\\s+(.+?)\\s*$");
  private static final Pattern ITEM_MARKDOWN = Pattern.compile("^\\s*(?:[-*+]\\s+|\\d+[.)]\\s+)(.+?)\\s*$");

  static AiBriefingDocument interpretar(String briefing) {
    String texto = briefing == null ? "" : briefing.replace("\r", "").trim();
    if (texto.isBlank()) {
      return new AiBriefingDocument("Página gerada", List.of());
    }

    String titulo = null;
    List<Secao> secoes = new ArrayList<>();
    String secaoAtual = "Visão geral";
    List<String> paragrafos = new ArrayList<>();
    List<String> itens = new ArrayList<>();
    StringBuilder paragrafoAtual = new StringBuilder();

    for (String linhaOriginal : texto.split("\\n", -1)) {
      String linha = linhaOriginal.trim();
      Matcher heading = TITULO_MARKDOWN.matcher(linha);
      if (heading.matches()) {
        fecharParagrafo(paragrafoAtual, paragrafos);
        if (!paragrafos.isEmpty() || !itens.isEmpty()) {
          secoes.add(new Secao(secaoAtual, List.copyOf(paragrafos), List.copyOf(itens)));
          paragrafos = new ArrayList<>();
          itens = new ArrayList<>();
        }
        String headingTexto = limparInline(heading.group(2));
        if (heading.group(1).length() == 1 && titulo == null) {
          titulo = headingTexto;
          secaoAtual = "Visão geral";
        } else {
          secaoAtual = headingTexto;
        }
        continue;
      }

      Matcher item = ITEM_MARKDOWN.matcher(linha);
      if (item.matches()) {
        fecharParagrafo(paragrafoAtual, paragrafos);
        itens.add(limparInline(item.group(1)).replaceFirst(";\\s*$", ""));
        continue;
      }

      if (linha.isBlank()) {
        fecharParagrafo(paragrafoAtual, paragrafos);
        continue;
      }

      if (!paragrafoAtual.isEmpty()) {
        paragrafoAtual.append(' ');
      }
      paragrafoAtual.append(limparInline(linha));
    }

    fecharParagrafo(paragrafoAtual, paragrafos);
    if (!paragrafos.isEmpty() || !itens.isEmpty()) {
      secoes.add(new Secao(secaoAtual, List.copyOf(paragrafos), List.copyOf(itens)));
    }

    if (titulo == null) {
      titulo = texto.lines()
          .map(String::trim)
          .filter(linha -> !linha.isBlank())
          .findFirst()
          .map(AiBriefingDocument::limparInline)
          .orElse("Página gerada");
    }
    return new AiBriefingDocument(titulo, List.copyOf(secoes));
  }

  Optional<Secao> encontrar(String... aliases) {
    for (String alias : aliases) {
      String termo = normalizar(alias);
      if (termo.isBlank()) {
        continue;
      }
      for (Secao secao : secoes) {
        if (normalizar(secao.titulo()).contains(termo)) {
          return Optional.of(secao);
        }
      }
    }
    return Optional.empty();
  }

  Optional<Secao> melhorPara(String metadados) {
    String fonte = normalizar(metadados);
    Secao melhor = null;
    int melhorScore = 0;
    for (Secao secao : secoes) {
      int score = 0;
      for (String token : normalizar(secao.titulo()).split("\\s+")) {
        if (token.length() >= 4 && fonte.contains(token)) {
          score += token.length();
        }
      }
      if (score > melhorScore) {
        melhor = secao;
        melhorScore = score;
      }
    }
    return Optional.ofNullable(melhor);
  }

  private static void fecharParagrafo(StringBuilder atual, List<String> paragrafos) {
    if (!atual.isEmpty()) {
      paragrafos.add(atual.toString().trim());
      atual.setLength(0);
    }
  }

  private static String limparInline(String value) {
    return value == null ? "" : value
        .replaceAll("^>\\s*", "")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .trim();
  }

  static String normalizar(String value) {
    String semAcentos = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  record Secao(String titulo, List<String> paragrafos, List<String> itens) {

    List<String> trechos() {
      List<String> trechos = new ArrayList<>(paragrafos);
      trechos.addAll(itens);
      return List.copyOf(trechos);
    }

    String conteudo() {
      List<String> partes = new ArrayList<>(paragrafos);
      if (!itens.isEmpty()) {
        partes.add(String.join("; ", itens) + ".");
      }
      return String.join(" ", partes).trim();
    }
  }
}
