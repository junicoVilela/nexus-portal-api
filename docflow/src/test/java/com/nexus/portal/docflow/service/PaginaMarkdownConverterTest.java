package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.service.PaginaMarkdownConverter.Opcoes;
import org.junit.jupiter.api.Test;

class PaginaMarkdownConverterTest {

  private static final Opcoes LINKS = new Opcoes(
      codigo -> codigo + ".md", (src, alt) -> "![" + alt + "](" + src + ")");

  @Test
  void titulosListasEnfaseViramMarkdown() {
    String md = PaginaMarkdownConverter.converter("""
        <section class="doc-section"><h2>Pré-requisitos</h2>
        <ul><li>Ter o perfil <strong>Operador</strong></li><li>Acesso a <em>Vendas</em>
          <ul><li>Leitura de pedidos</li></ul></li></ul>
        <h3>Passo a passo</h3><ol><li>Filtre por período.</li><li>Clique em <code>Exportar</code>.</li></ol></section>
        """, LINKS);

    assertThat(md).isEqualTo("""
        ## Pré-requisitos

        - Ter o perfil **Operador**
        - Acesso a *Vendas*
          - Leitura de pedidos

        ### Passo a passo

        1. Filtre por período.
        2. Clique em `Exportar`.""");
  }

  @Test
  void tabelaViraGfmComCabecalho() {
    String md = PaginaMarkdownConverter.converter("""
        <div class="table-wrap"><table><thead><tr><th>Campo</th><th>Obrigatório</th></tr></thead>
        <tbody><tr><td>Período</td><td><span class="status-badge status-badge--sim">Sim</span></td></tr>
        <tr><td>Status a|b</td><td>Não</td></tr></tbody></table></div>
        """, LINKS);

    assertThat(md).isEqualTo("""
        | Campo | Obrigatório |
        | --- | --- |
        | Período | Sim |
        | Status a\\|b | Não |""");
  }

  @Test
  void avisoViraCitacaoEPlaceholderDeCapturaSome() {
    String md = PaginaMarkdownConverter.converter("""
        <div class="callout warning"><p>Pedidos faturados não podem ser editados.</p></div>
        <figure class="screen-frame"><div class="screen-placeholder"><p>Insira aqui uma captura</p></div>
        <figcaption>Tela de consulta</figcaption></figure>
        """, LINKS);

    assertThat(md).contains("> **Atenção:**\n> Pedidos faturados não podem ser editados.")
        .contains("*Tela de consulta*")
        .doesNotContain("Insira aqui");
  }

  @Test
  void linksEntreTelasEImagensSeguemAsOpcoes() {
    String html = "<p>Veja <span data-codigo-tela=\"PED-002\">Detalhe do pedido</span> e"
        + " <img src=\"../assets/a.png\" alt=\"Filtros\"> <a href=\"javascript:alert(1)\">x</a></p>";

    assertThat(PaginaMarkdownConverter.converter(html, LINKS))
        .isEqualTo("Veja [Detalhe do pedido](PED-002.md) e ![Filtros](../assets/a.png) x");
    assertThat(PaginaMarkdownConverter.converter(html, Opcoes.texto()))
        .isEqualTo("Veja Detalhe do pedido e [Imagem: Filtros] x");
  }

  @Test
  void escapaMarcacaoMarkdownDoTextoEDemoteH1() {
    assertThat(PaginaMarkdownConverter.converter("<h1>Total *bruto*</h1><p>use_snake</p>", LINKS))
        .isEqualTo("## Total \\*bruto\\*\n\nuse\\_snake");
  }
}
