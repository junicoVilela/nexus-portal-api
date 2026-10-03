package com.nexus.portal.ai.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Esboço de uma página existente para o ajuste com IA (Fase B): seções e unidades de texto com
 * ids posicionais ({@code s1}, {@code u1}…), sobre o qual o modelo propõe um patch.
 *
 * <p>O agrupamento em seções é o mesmo de {@code extrairSecoesPagina} no front
 * ({@code pagina-section-organizer}): um título {@code h1–h6} solto abre uma seção que absorve os
 * nós seguintes; {@code section}/{@code article} é uma seção inteira; os demais nós sem título
 * aberto viram seções próprias. Assim autor e modelo enxergam as mesmas seções.
 *
 * <p>Ids só valem para o HTML exato que gerou o esboço — por isso a sessão guarda a versão base.
 */
public final class AiPaginaEsboco {

  private static final Set<String> TITULOS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
  private static final Set<String> SECOES = Set.of("section", "article");
  private static final Set<String> UNIDADES = Set.of(
      "h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "td", "th", "dt", "dd", "figcaption", "blockquote");
  private static final int RESUMO_TITULO_SECAO = 60;

  private final Document documento;
  private final List<Secao> secoes;
  private final Map<String, Unidade> unidades;

  private AiPaginaEsboco(Document documento, List<Secao> secoes) {
    this.documento = documento;
    this.secoes = List.copyOf(secoes);
    Map<String, Unidade> porId = new LinkedHashMap<>();
    secoes.forEach(secao -> secao.unidades().forEach(unidade -> porId.put(unidade.id(), unidade)));
    this.unidades = porId;
  }

  public static AiPaginaEsboco de(String html) {
    Document documento = Jsoup.parseBodyFragment(html == null ? "" : html);
    documento.outputSettings().prettyPrint(false);
    List<List<Node>> grupos = agrupar(documento.body().childNodes());
    List<Secao> secoes = new ArrayList<>();
    int proximaUnidade = 1;
    for (List<Node> grupo : grupos) {
      List<Unidade> unidades = new ArrayList<>();
      for (Node no : grupo) {
        if (no instanceof Element elemento) {
          for (Element candidato : unidadesDe(elemento)) {
            String texto = candidato.text().trim();
            if (texto.isEmpty()) {
              continue;
            }
            unidades.add(new Unidade(
                "u" + proximaUnidade++,
                candidato.tagName(),
                texto,
                candidato.children().isEmpty(),
                TITULOS.contains(candidato.tagName()),
                candidato));
          }
        }
      }
      secoes.add(new Secao("s" + (secoes.size() + 1), tituloSecao(grupo, unidades), grupo, unidades));
    }
    return new AiPaginaEsboco(documento, secoes);
  }

  public Document documento() {
    return documento;
  }

  public List<Secao> secoes() {
    return secoes;
  }

  public Optional<Secao> secao(String id) {
    return secoes.stream().filter(secao -> secao.id().equals(id)).findFirst();
  }

  public Optional<Unidade> unidade(String id) {
    return Optional.ofNullable(unidades.get(id));
  }

  public int totalUnidades() {
    return unidades.size();
  }

  public int totalCaracteres() {
    return unidades.values().stream().mapToInt(unidade -> unidade.texto().length()).sum();
  }

  /**
   * Texto enviado ao modelo. {@code secaoId} limita às unidades daquela seção; os ids continuam
   * os globais, para o patch apontar para o mesmo lugar.
   */
  public String descrever(String secaoId) {
    StringBuilder out = new StringBuilder();
    for (Secao secao : secoes) {
      if (secaoId != null && !secaoId.equals(secao.id())) {
        continue;
      }
      out.append(secao.id()).append("  \"").append(secao.titulo()).append("\"\n");
      for (Unidade unidade : secao.unidades()) {
        out.append("    ").append(unidade.id()).append("  [").append(unidade.tag()).append("]  ")
            .append(unidade.texto().replaceAll("\\s+", " "));
        if (!unidade.editavel()) {
          out.append("  (somente leitura: contém link ou formatação)");
        }
        out.append('\n');
      }
    }
    return out.toString();
  }

  private static List<List<Node>> agrupar(List<Node> filhos) {
    List<List<Node>> grupos = new ArrayList<>();
    List<Node> grupoTitulo = new ArrayList<>();
    for (Node no : List.copyOf(filhos)) {
      if (no instanceof TextNode texto && texto.isBlank()) {
        continue;
      }
      if (!(no instanceof Element) && !(no instanceof TextNode)) {
        continue;
      }
      Element elemento = no instanceof Element el ? el : null;
      if (elemento != null && TITULOS.contains(elemento.tagName())) {
        if (!grupoTitulo.isEmpty()) {
          grupos.add(grupoTitulo);
        }
        grupoTitulo = new ArrayList<>(List.of(no));
        continue;
      }
      if (elemento != null && SECOES.contains(elemento.tagName())) {
        if (!grupoTitulo.isEmpty()) {
          grupos.add(grupoTitulo);
          grupoTitulo = new ArrayList<>();
        }
        grupos.add(List.of(no));
        continue;
      }
      if (!grupoTitulo.isEmpty()) {
        grupoTitulo.add(no);
        continue;
      }
      grupos.add(List.of(no));
    }
    if (!grupoTitulo.isEmpty()) {
      grupos.add(grupoTitulo);
    }
    return grupos;
  }

  /** Unidade = elemento de texto que não contém outra unidade (evita contar o mesmo texto duas vezes). */
  private static List<Element> unidadesDe(Element raiz) {
    List<Element> encontradas = new ArrayList<>();
    for (Element elemento : raiz.select("*")) {
      if (UNIDADES.contains(elemento.tagName()) && !contemUnidade(elemento)) {
        encontradas.add(elemento);
      }
    }
    return encontradas;
  }

  private static boolean contemUnidade(Element elemento) {
    for (Element descendente : elemento.select("*")) {
      if (descendente != elemento && UNIDADES.contains(descendente.tagName())) {
        return true;
      }
    }
    return false;
  }

  private static String tituloSecao(List<Node> grupo, List<Unidade> unidades) {
    return unidades.stream()
        .filter(Unidade::titulo)
        .map(Unidade::texto)
        .findFirst()
        .or(() -> unidades.stream().map(Unidade::texto).findFirst())
        .map(texto -> texto.length() <= RESUMO_TITULO_SECAO
            ? texto
            : texto.substring(0, RESUMO_TITULO_SECAO) + "…")
        .orElseGet(() -> grupo.getFirst() instanceof Element el ? el.tagName() : "texto");
  }

  /** {@code nos}: nós de primeiro nível da seção, na ordem do documento. */
  public record Secao(String id, String titulo, List<Node> nos, List<Unidade> unidades) {

    public Node ultimoNo() {
      return nos.getLast();
    }
  }

  /**
   * {@code editavel}: só texto, sem elementos filhos — trocar o texto não apaga link nem
   * formatação. {@code titulo}: cabeçalho {@code h1–h6}, que nunca é removido.
   */
  public record Unidade(
      String id, String tag, String texto, boolean editavel, boolean titulo, Element elemento) {}
}
