package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarkdownPdfRendererTest {

  private final MarkdownPdfRenderer renderer = new MarkdownPdfRenderer();

  @Test
  void renderToHtml_substituiVariaveisThymeleaf() {
    String template = "# Release [(${produto})] [(${versao})]\n\nCliente: [(${cliente})]";
    String html = renderer.renderToHtml(template,
        Map.of("produto", "NEXUS-LD", "versao", "1.5.0", "cliente", "ACME"));

    assertThat(html).contains("<h1>Release NEXUS-LD 1.5.0</h1>");
    assertThat(html).contains("Cliente: ACME");
  }

  @Test
  void renderToHtml_renderizaListaMarkdown() {
    String template = """
        ## Novidades

        [# th:each="item : ${itens}"]
        - [(${item})]
        [/]
        """;
    String html = renderer.renderToHtml(template,
        Map.of("itens", List.of("CSV export", "SSO")));

    assertThat(html).contains("<h2>Novidades</h2>");
    assertThat(html).contains("<li>CSV export</li>");
    assertThat(html).contains("<li>SSO</li>");
  }

  @Test
  void renderToHtml_renderizaTabelaGfm() {
    String template = """
        | Módulo | Versão |
        |--------|--------|
        | portal | 1.5.0  |
        | api    | 1.5.1  |
        """;
    String html = renderer.renderToHtml(template, Map.of());

    assertThat(html).contains("<table>");
    assertThat(html).contains("<th>Módulo</th>");
    assertThat(html).contains("<td>portal</td>");
  }

  @Test
  void renderToHtml_envolveCorpoComCssDeImpressao() {
    String html = renderer.renderToHtml("hello", Map.of());
    assertThat(html).contains("<!DOCTYPE html>");
    assertThat(html).contains("font-family");
    assertThat(html).contains("<p>hello</p>");
  }

  @Test
  void renderToHtml_aceitaTemplateVazio() {
    String html = renderer.renderToHtml("", Map.of());
    assertThat(html).contains("<body>");
    assertThat(html).contains("</body>");
  }

  @Test
  void renderToPdf_geraBytesPdfValidos() {
    byte[] pdf = renderer.renderToPdf("# Hello\n\nWorld", Map.of());

    assertThat(pdf).isNotEmpty();
    // PDF começa com magic bytes "%PDF-"
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }
}
