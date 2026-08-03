package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Pagina;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class PaginaQualidadeService {

  private static final Pattern PLACEHOLDER = Pattern.compile(
      "\\{\\{\\s*[a-zA-Z0-9_.-]+\\s*}}|\\b(explique|descreva|informe|liste|registre aqui|nome do campo|escreva uma resposta)\\b",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  public ResultadoQualidade avaliar(Pagina pagina) {
    String html = pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml();
    Document document = Jsoup.parseBodyFragment(html);
    String texto = extrairTexto(document);
    Matcher placeholderMatcher = PLACEHOLDER.matcher(texto);
    String placeholderEncontrado = placeholderMatcher.find() ? placeholderMatcher.group() : null;

    boolean temScreenPlaceholder = !document.select(".screen-placeholder").isEmpty();
    boolean temImagem = !document.select("img").isEmpty();
    boolean temImagemEmFrame = !document.select(".screen-frame img").isEmpty();
    boolean temSteps = !document.select(".steps").isEmpty() || texto.toLowerCase(Locale.ROOT).contains("passo a passo");
    boolean temResultCard = !document.select(".result-card").isEmpty();
    boolean temChecklist = !document.select(".checklist").isEmpty();
    boolean temPreRequisitos = texto.toLowerCase(Locale.ROOT).contains("pré-requisitos");

    List<String> codigosVerTambem = document.select("[data-codigo-tela]").stream()
        .map(elemento -> elemento.attr("data-codigo-tela").trim())
        .filter(valor -> !valor.isEmpty())
        .toList();
    long codigosVerTambemInvalidos = codigosVerTambem.stream()
        .filter(valor -> valor.isEmpty() || valor.toUpperCase(Locale.ROOT).startsWith("CODIGO"))
        .count();

    List<ItemQualidade> itens = new ArrayList<>();

    itens.add(item("TITULO", "Título definido", "Informe um título claro para a página.",
        pagina.getTitulo() != null && !pagina.getTitulo().isBlank(), Severidade.ERRO));
    itens.add(item("CODIGO_TELA", "Código da tela definido", "Vincule a documentação à tela correta.",
        pagina.getCodigoTela() != null && !pagina.getCodigoTela().isBlank(), Severidade.ERRO));
    itens.add(item("CONTEUDO", "Conteúdo desenvolvido",
        "A página precisa ter pelo menos 80 caracteres de conteúdo útil.", texto.length() >= 80,
        Severidade.ERRO));
    itens.add(item("PLACEHOLDERS", "Textos de orientação substituídos",
        placeholderEncontrado != null
            ? "Substitua ou remova “" + placeholderEncontrado + "” do conteúdo."
            : "Não há instruções de modelo pendentes no conteúdo.",
        placeholderEncontrado == null, Severidade.ERRO));
    itens.add(item("IMAGENS_ALT", "Imagens acessíveis", "Toda imagem deve possuir texto alternativo.",
        document.select("img").stream().allMatch(img -> !img.attr("alt").isBlank()), Severidade.ERRO));
    itens.add(item("IMAGENS_ORIGEM", "Imagens disponíveis",
        "Toda imagem deve possuir uma origem válida.",
        document.select("img").stream().allMatch(img -> !img.attr("src").isBlank()), Severidade.ERRO));
    itens.add(item("LINKS", "Links válidos",
        "Links não podem estar vazios nem usar endereços JavaScript.",
        document.select("a").stream().allMatch(link -> linkValido(link.attr("href"))), Severidade.ERRO));
    itens.add(item("TITULOS", "Hierarquia de títulos consistente",
        "Organize as seções sem saltar níveis de título.", titulosConsistentes(document), Severidade.AVISO));
    itens.add(item("RESUMO", "Resumo preenchido", "Inclua uma descrição curta para buscas e navegação.",
        pagina.getResumo() != null && pagina.getResumo().trim().length() >= 30, Severidade.AVISO));
    itens.add(item("SECOES", "Conteúdo organizado em seções",
        "Use ao menos um título de seção para facilitar a leitura.", !document.select("h2, h3").isEmpty(),
        Severidade.AVISO));
    itens.add(item("CAPTURA", "Captura de tela inserida",
        temScreenPlaceholder && !temImagemEmFrame && !temImagem
            ? "Substitua o placeholder por uma captura real da tela."
            : "Captura de tela presente ou sem placeholder pendente.",
        !temScreenPlaceholder || temImagemEmFrame || temImagem, Severidade.AVISO));
    itens.add(item("RESULTADO", "Resultado esperado documentado",
        temSteps && !temResultCard
            ? "Inclua um bloco de resultado esperado após o passo a passo."
            : "Resultado esperado presente ou passo a passo não utilizado.",
        !temSteps || temResultCard, Severidade.AVISO));
    itens.add(item("VER_TAMBEM", "Links “Ver também” com código válido",
        !codigosVerTambem.isEmpty() && codigosVerTambemInvalidos > 0
            ? "Substitua códigos genéricos ou vazios em data-codigo-tela."
            : "Links “Ver também” com códigos válidos ou não utilizados.",
        codigosVerTambem.isEmpty() || codigosVerTambemInvalidos == 0, Severidade.AVISO));
    itens.add(item("PRE_REQS", "Pré-requisitos documentados",
        temSteps && !temChecklist && !temPreRequisitos
            ? "Inclua pré-requisitos ou checklist antes do passo a passo."
            : "Pré-requisitos presentes ou passo a passo não utilizado.",
        !temSteps || temChecklist || temPreRequisitos, Severidade.AVISO));

    boolean apto = itens.stream().noneMatch(item -> item.severidade() == Severidade.ERRO && !item.ok());
    return new ResultadoQualidade(apto, List.copyOf(itens));
  }

  private String extrairTexto(Document document) {
    document.select("h1, h2, h3, h4, h5, h6, p, li, td, th, br").forEach(element -> element.append(" "));
    return document.body().text().replaceAll("\\s+", " ").trim();
  }

  private ItemQualidade item(String codigo, String titulo, String descricao, boolean ok,
      Severidade severidade) {
    return new ItemQualidade(codigo, titulo, descricao, ok, severidade);
  }

  private boolean linkValido(String href) {
    if (href == null || href.isBlank()) {
      return false;
    }
    String normalizado = href.trim().toLowerCase(Locale.ROOT);
    return !normalizado.startsWith("javascript:") && !"#".equals(normalizado);
  }

  private boolean titulosConsistentes(Document document) {
    int nivelAnterior = 0;
    for (var titulo : document.select("h1, h2, h3, h4, h5, h6")) {
      int nivelAtual = Integer.parseInt(titulo.tagName().substring(1));
      if (nivelAnterior > 0 && nivelAtual > nivelAnterior + 1) {
        return false;
      }
      nivelAnterior = nivelAtual;
    }
    return true;
  }

  public enum Severidade {
    ERRO,
    AVISO
  }

  public record ItemQualidade(
      String codigo,
      String titulo,
      String descricao,
      boolean ok,
      Severidade severidade) {
  }

  public record ResultadoQualidade(boolean aptoParaRevisao, List<ItemQualidade> itens) {
  }
}
