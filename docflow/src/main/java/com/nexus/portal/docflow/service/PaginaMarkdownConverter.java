package com.nexus.portal.docflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Converte o HTML de uma página (blocos do DocFlow) em Markdown limpo para RAG e {@code llms-full.txt}.
 *
 * <p>Preserva o que importa para recuperar e citar: hierarquia de títulos (os chunkers cortam por
 * {@code ##}), listas, tabelas (GFM), avisos e links entre telas. Descarta o que é só visual:
 * placeholders de captura, grades, ícones e classes.
 */
public final class PaginaMarkdownConverter {

  private static final Set<String> BLOCOS = Set.of(
      "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "table", "blockquote", "pre", "hr",
      "figure", "figcaption", "div", "section", "article", "aside", "header", "footer", "main", "nav",
      "details", "summary", "dl", "dt", "dd");

  private PaginaMarkdownConverter() {}

  /**
   * @param linkTela código de tela → destino do link (nulo = só o texto)
   * @param imagem (src, alt) → Markdown da imagem (nulo = descarta)
   */
  public record Opcoes(Function<String, String> linkTela, BiFunction<String, String, String> imagem) {

    /** Sem links entre telas; imagens viram só a descrição (texto puro para RAG). */
    public static Opcoes texto() {
      return new Opcoes(codigo -> null, (src, alt) -> alt.isBlank() ? null : "[Imagem: " + alt + "]");
    }
  }

  public static String converter(String html, Opcoes opcoes) {
    if (html == null || html.isBlank()) {
      return "";
    }
    Element body = Jsoup.parseBodyFragment(html).body();
    StringBuilder out = new StringBuilder();
    new Escritor(opcoes, out).blocos(body, "");
    return out.toString()
        .replaceAll("[ \\t]+\\n", "\n")
        // Linha ">" vazia que fecha uma citação é só ruído.
        .replaceAll("\\n>\\n(?=\\n|$)", "\n")
        .replaceAll("\\n{3,}", "\n\n")
        .strip();
  }

  private record Escritor(Opcoes opcoes, StringBuilder out) {

    void blocos(Element pai, String prefixo) {
      StringBuilder inline = new StringBuilder();
      for (Node filho : pai.childNodes()) {
        if (filho instanceof Element el && BLOCOS.contains(el.normalName())) {
          paragrafo(inline, prefixo);
          bloco(el, prefixo);
        } else {
          inline.append(inline(filho, false));
        }
      }
      paragrafo(inline, prefixo);
    }

    private void bloco(Element el, String prefixo) {
      if (el.hasClass("screen-placeholder")) {
        return; // "insira a captura aqui" não é conteúdo
      }
      String tag = el.normalName();
      switch (tag) {
        case "h1", "h2", "h3", "h4", "h5", "h6" -> {
          // O H1 é o título da página; títulos do conteúdo começam em ##.
          int nivel = Math.max(2, tag.charAt(1) - '0');
          linha(prefixo, "#".repeat(nivel) + " " + texto(el));
        }
        case "p", "summary", "dt" -> {
          String texto = inlineDe(el, false).strip();
          if (!texto.isEmpty()) {
            linha(prefixo, "summary".equals(tag) || "dt".equals(tag) ? "**" + texto + "**" : texto);
          }
        }
        case "ul", "ol" -> lista(el, prefixo, 0);
        case "table" -> tabela(el, prefixo);
        case "pre" -> linha(prefixo, "```\n" + el.wholeText().strip() + "\n```");
        case "hr" -> linha(prefixo, "---");
        case "blockquote" -> blocos(el, prefixo + "> ");
        case "figcaption" -> {
          String legenda = texto(el);
          if (!legenda.isEmpty()) {
            linha(prefixo, "*" + legenda + "*");
          }
        }
        default -> {
          String rotulo = rotuloAviso(el);
          if (rotulo != null) {
            out.append(prefixo).append("> **").append(rotulo).append(":**\n");
            blocos(el, prefixo + "> ");
            out.append('\n');
          } else {
            blocos(el, prefixo);
          }
        }
      }
    }

    private void lista(Element lista, String prefixo, int nivel) {
      boolean ordenada = "ol".equals(lista.normalName());
      int numero = 1;
      String recuo = "  ".repeat(nivel);
      for (Element item : lista.children()) {
        if (!"li".equals(item.normalName())) {
          continue;
        }
        StringBuilder texto = new StringBuilder();
        List<Element> sublistas = new ArrayList<>();
        for (Node filho : item.childNodes()) {
          if (filho instanceof Element el && ("ul".equals(el.normalName()) || "ol".equals(el.normalName()))) {
            sublistas.add(el);
          } else if (filho instanceof Element el && BLOCOS.contains(el.normalName())) {
            texto.append(' ').append(inlineDe(el, false));
          } else {
            texto.append(inline(filho, false));
          }
        }
        String marcador = ordenada ? (numero++) + ". " : "- ";
        out.append(prefixo).append(recuo).append(marcador).append(colapsar(texto.toString())).append('\n');
        sublistas.forEach(sub -> lista(sub, prefixo, nivel + 1));
      }
      if (nivel == 0) {
        out.append(prefixo.stripTrailing()).append('\n');
      }
    }

    private void tabela(Element tabela, String prefixo) {
      List<List<String>> linhas = new ArrayList<>();
      boolean cabecalho = false;
      for (Element tr : tabela.select("tr")) {
        List<String> celulas = new ArrayList<>();
        for (Element celula : tr.children()) {
          if ("th".equals(celula.normalName()) || "td".equals(celula.normalName())) {
            celulas.add(inlineDe(celula, true).strip().replace("|", "\\|"));
            cabecalho |= linhas.isEmpty() && "th".equals(celula.normalName());
          }
        }
        if (!celulas.isEmpty()) {
          linhas.add(celulas);
        }
      }
      if (linhas.isEmpty()) {
        return;
      }
      int colunas = linhas.stream().mapToInt(List::size).max().orElse(1);
      List<String> topo = cabecalho ? linhas.removeFirst() : List.of();
      out.append(prefixo).append(linhaTabela(topo, colunas)).append('\n');
      out.append(prefixo).append("|").append(" --- |".repeat(colunas)).append('\n');
      linhas.forEach(l -> out.append(prefixo).append(linhaTabela(l, colunas)).append('\n'));
      out.append(prefixo.stripTrailing()).append('\n');
    }

    private static String linhaTabela(List<String> celulas, int colunas) {
      StringBuilder linha = new StringBuilder("|");
      for (int i = 0; i < colunas; i++) {
        linha.append(' ').append(i < celulas.size() ? celulas.get(i) : "").append(" |");
      }
      return linha.toString();
    }

    private String inlineDe(Element el, boolean emTabela) {
      StringBuilder sb = new StringBuilder();
      el.childNodes().forEach(filho -> sb.append(inline(filho, emTabela)));
      return colapsar(sb.toString());
    }

    private String inline(Node node, boolean emTabela) {
      if (node instanceof TextNode texto) {
        return escapar(texto.getWholeText().replaceAll("\\s+", " "));
      }
      if (!(node instanceof Element el)) {
        return "";
      }
      String codigoTela = el.attr("data-codigo-tela");
      if (!codigoTela.isBlank()) {
        String destino = opcoes.linkTela().apply(codigoTela);
        String rotulo = colapsar(el.text());
        return " " + (destino == null ? rotulo : "[" + rotulo + "](" + destino + ")") + " ";
      }
      return switch (el.normalName()) {
        case "strong", "b" -> envolver(inlineDe(el, emTabela), "**");
        case "em", "i" -> envolver(inlineDe(el, emTabela), "*");
        case "code" -> "`" + el.text().replace("`", "'") + "`";
        case "br" -> emTabela ? " " : "\n";
        case "img" -> {
          String md = opcoes.imagem().apply(el.attr("src"), el.attr("alt").strip());
          yield md == null ? "" : md;
        }
        case "a" -> {
          String rotulo = inlineDe(el, emTabela);
          String href = el.attr("href").strip();
          boolean valido = !href.isEmpty() && !href.toLowerCase(Locale.ROOT).startsWith("javascript:");
          yield valido && !rotulo.isBlank() ? "[" + rotulo + "](" + href + ")" : rotulo;
        }
        case "script", "style" -> "";
        default -> BLOCOS.contains(el.normalName()) ? " " + inlineDe(el, emTabela) + " " : inlineDe(el, emTabela);
      };
    }

    private void paragrafo(StringBuilder inline, String prefixo) {
      String texto = colapsar(inline.toString());
      inline.setLength(0);
      if (!texto.isEmpty()) {
        linha(prefixo, texto);
      }
    }

    private void linha(String prefixo, String texto) {
      for (String parte : texto.split("\n", -1)) {
        out.append(prefixo).append(parte).append('\n');
      }
      out.append(prefixo.stripTrailing()).append('\n');
    }

    /** Texto puro escapado (títulos e legendas não levam ênfase interna). */
    private String texto(Element el) {
      return escapar(colapsar(el.text()));
    }

    /** Avisos dos blocos (callout, warning) viram citação com rótulo. */
    private static String rotuloAviso(Element el) {
      if (el.hasClass("warning") || el.hasClass("callout--danger")) {
        return "Atenção";
      }
      if (el.hasClass("callout")) {
        return "Dica";
      }
      return null;
    }

    private static String envolver(String texto, String marca) {
      String limpo = texto.strip();
      return limpo.isEmpty() ? "" : marca + limpo + marca;
    }

    private static String colapsar(String texto) {
      // Preserva quebras explícitas (<br>), junta o resto.
      return texto.replaceAll("[ \\t]+", " ").replaceAll(" *\\n *", "\n").strip();
    }

    private static String escapar(String texto) {
      return texto.replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_").replace("`", "\\`");
    }
  }
}
