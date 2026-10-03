package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiPaginaEsbocoTest {

  /** Mesma estrutura usada em extrairSecoesPagina (pagina-section-organizer) no front. */
  static final String HTML = """
      <section class="doc-intro"><h1>Consulta de pedidos</h1><p>Use esta tela para localizar pedidos.</p></section>
      <h2>Pré-requisitos</h2>
      <ul><li>Ter o perfil Operador.</li><li>Acesso ao <a href="/vendas">módulo Vendas</a>.</li></ul>
      <h2>Passo a passo</h2>
      <ol><li><p>Filtre por período.</p></li><li>Clique em Exportar.</li></ol>
      <figure><img src="/api/v1/paginas/1/anexos/2/download" alt="Tela"><figcaption>Tela de consulta</figcaption></figure>
      <p>Texto solto final.</p>
      """;

  @Test
  void agrupaSecoesComoOEditor() {
    AiPaginaEsboco esboco = AiPaginaEsboco.de(HTML);

    assertThat(esboco.secoes()).extracting(AiPaginaEsboco.Secao::titulo)
        .containsExactly("Consulta de pedidos", "Pré-requisitos", "Passo a passo");
    assertThat(esboco.secoes().get(2).unidades()).extracting(AiPaginaEsboco.Unidade::texto)
        .containsExactly("Passo a passo", "Filtre por período.", "Clique em Exportar.", "Tela de consulta",
            "Texto solto final.");
  }

  @Test
  void idsSaoPosicionaisEEstaveis() {
    var primeiro = AiPaginaEsboco.de(HTML);
    var segundo = AiPaginaEsboco.de(HTML);
    assertThat(primeiro.descrever(null)).isEqualTo(segundo.descrever(null));
    assertThat(primeiro.unidade("u1").orElseThrow().texto()).isEqualTo("Consulta de pedidos");
  }

  @Test
  void unidadeComLinkFicaSomenteLeituraETituloEhMarcado() {
    AiPaginaEsboco esboco = AiPaginaEsboco.de(HTML);
    var comLink = esboco.secoes().get(1).unidades().get(2);

    assertThat(comLink.texto()).contains("módulo Vendas");
    assertThat(comLink.editavel()).isFalse();
    assertThat(esboco.secoes().get(1).unidades().get(0).titulo()).isTrue();
    assertThat(esboco.descrever(null)).contains("(somente leitura");
  }

  @Test
  void naoContaOMesmoTextoDuasVezesEmElementosAninhados() {
    AiPaginaEsboco esboco = AiPaginaEsboco.de("<ol><li><p>Filtre por período.</p></li></ol>");
    assertThat(esboco.totalUnidades()).isEqualTo(1);
    assertThat(esboco.unidade("u1").orElseThrow().tag()).isEqualTo("p");
  }

  @Test
  void descreverComSecaoMantemIdsGlobais() {
    String descricao = AiPaginaEsboco.de(HTML).descrever("s2");
    assertThat(descricao).startsWith("s2").contains("u3").doesNotContain("u1 ");
  }
}
