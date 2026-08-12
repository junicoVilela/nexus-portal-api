package com.nexus.portal.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FakeLlmProviderTest {

  private final FakeLlmProvider provider = new FakeLlmProvider();

  @Test
  void completarPadraoRetornaPerguntas() {
    var out = provider.completar("system", "user");
    assertThat(provider.id()).isEqualTo(FakeLlmProvider.ID);
    assertThat(out.content()).contains("PERGUNTAS").contains("codigoTela");
  }

  @Test
  void completarGeracaoRetornaRascunhoJson() {
    var out = provider.completar(
        "GERAR_RASCUNHO",
        """
            titulo: Consulta de pedidos
            codigoTela: PED-CONSULTA
            resumo: Como filtrar e exportar pedidos.
            """);

    assertThat(out.content()).contains("conteudoHtml");
    assertThat(out.content()).contains("PED-CONSULTA");
    assertThat(out.content()).contains("doc-intro");
    assertThat(out.tokensEntrada()).isNotNull();
  }

  @Test
  void completarUsaEsqueletoDaBibliotecaQuandoPresente() {
    var out = provider.completar(
        "GERAR_RASCUNHO",
        """
            titulo: Consulta
            codigoTela: PED-001
            resumo: Resumo
            esqueletoHtml:
            <section class="modelo-biblioteca"><p>Do template</p></section>
            INSTRUCAO_FINAL: preencher
            """);

    assertThat(out.content()).contains("modelo-biblioteca");
    assertThat(out.content()).contains("Do template");
    assertThat(out.content()).doesNotContain("Guia da funcionalidade");
  }
}
