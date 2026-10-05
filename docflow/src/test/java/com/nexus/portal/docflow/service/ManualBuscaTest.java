package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.service.ManualCorpusService.Corpus;
import com.nexus.portal.docflow.service.ManualCorpusService.Documento;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualBuscaTest {

  private static Corpus corpus() {
    Map<String, Documento> docs = new LinkedHashMap<>();
    docs.put("PED-001", new Documento("PED-001", "Consulta de pedidos", "Vendas › Consulta de pedidos",
        "paginas/pedidos.html", """
        # Consulta de pedidos

        > Como localizar pedidos.

        ## Filtros

        Filtre os pedidos por período, status e vendedor.

        ## Exportação

        Clique em Exportar para gerar um arquivo CSV com os filtros aplicados.
        """));
    docs.put("CLI-001", new Documento("CLI-001", "Cadastro de clientes", "Cadastros › Clientes",
        "paginas/clientes.html", """
        # Cadastro de clientes

        ## Campos obrigatórios

        Informe CNPJ, razão social e e-mail de contato.
        """));
    var secoes = docs.values().stream().flatMap(d -> ManualCorpusService.secoes(d).stream()).toList();
    return new Corpus(UUID.randomUUID(), "ACME", "1.0", docs, secoes);
  }

  @Test
  void sinonimoDoClienteTrocaATermoDaPergunta() {
    var semSinonimo = ManualBusca.buscar(corpus(), "onde fica a planilha?", 3);
    assertThat(semSinonimo.encontrou()).isFalse();

    var comSinonimo = ManualBusca.buscar(corpus().comSinonimos(List.of(List.of("planilha", "csv"))),
        "onde fica a planilha?", 3);

    assertThat(comSinonimo.encontrou()).isTrue();
    assertThat(comSinonimo.resultados().getFirst().secao().titulo()).isEqualTo("Exportação");
  }

  @Test
  void achaASecaoCertaMesmoComFlexaoEAcento() {
    var resposta = ManualBusca.buscar(corpus(), "Como filtrar os pedidos por vendedor?", 3);

    assertThat(resposta.encontrou()).isTrue();
    assertThat(resposta.resultados().getFirst().secao().titulo()).isEqualTo("Filtros");
    assertThat(resposta.resultados().getFirst().secao().documento().codigoTela()).isEqualTo("PED-001");
  }

  @Test
  void exportacaoEExportarSeEncontram() {
    var resposta = ManualBusca.buscar(corpus(), "exportação de pedidos em csv", 3);
    assertThat(resposta.resultados().getFirst().secao().titulo()).isEqualTo("Exportação");
  }

  @Test
  void assuntoForaDoManualViraNaoSei() {
    var resposta = ManualBusca.buscar(corpus(), "Como emitir nota fiscal de devolução?", 3);
    assertThat(resposta.encontrou()).isFalse();
  }

  @Test
  void codigoDeTelaCitadoPuxaATela() {
    var resposta = ManualBusca.buscar(corpus(), "o que tem na cli-001", 3);
    assertThat(resposta.encontrou()).isTrue();
    assertThat(resposta.resultados().getFirst().secao().documento().codigoTela()).isEqualTo("CLI-001");
  }

  @Test
  void perguntaSoComPalavrasVaziasNaoBusca() {
    assertThat(ManualBusca.buscar(corpus(), "como é que eu faço isso?", 3).resultados()).isEmpty();
  }

  @Test
  void secoesCortadasPorTituloComIntroducao() {
    var secoes = ManualCorpusService.secoes(corpus().documentos().get("PED-001"));
    assertThat(secoes).extracting(s -> s.titulo()).containsExactly(null, "Filtros", "Exportação");
    assertThat(secoes.getFirst().texto()).isEqualTo("> Como localizar pedidos.");
  }

  @Test
  void radicalJuntaFormasDaMesmaPalavra() {
    assertThat(List.of("filtrar", "filtro", "filtros").stream().map(ManualBusca::radical).distinct()).hasSize(1);
    assertThat(ManualCorpusService.semFrontmatter("---\ncodigoTela: \"X\"\n---\n\n# T\n")).isEqualTo("# T");
  }
}
