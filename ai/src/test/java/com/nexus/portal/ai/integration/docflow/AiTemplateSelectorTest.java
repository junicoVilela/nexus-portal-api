package com.nexus.portal.ai.integration.docflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.entity.PaginaTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiTemplateSelectorTest {

  @Test
  void selecionaConsultaPorPalavrasChave() {
    var consulta = template("CONSULTA", "Consulta");
    var faq = template("FAQ", "FAQ");
    var hit = AiTemplateSelector.selecionar(
        "Tela de consulta com filtros e busca de pedidos",
        List.of(faq, consulta));
    assertThat(hit).isPresent();
    assertThat(hit.get().getCodigo()).isEqualTo("CONSULTA");
  }

  @Test
  void selecionaListarRegistros() {
    var lista = template("LISTAR_REGISTROS", "Lista");
    var hit = AiTemplateSelector.selecionar(
        "Listagem com grade e exportar CSV",
        List.of(lista, template("FUNCIONALIDADE", "Func")));
    assertThat(hit.get().getCodigo()).isEqualTo("LISTAR_REGISTROS");
  }

  @Test
  void prefereCadastroParaManualComCicloCrudCompleto() {
    var cadastro = template("CADASTRO", "Cadastro ou edição");
    var lista = template("LISTAR_REGISTROS", "Listar registros");
    var incluir = template("INCLUIR_REGISTRO", "Incluir registro");

    var hit = AiTemplateSelector.selecionar(
        "Manual para consultar e listar, cadastrar, editar e excluir registros.",
        List.of(lista, incluir, cadastro));

    assertThat(hit.get().getCodigo()).isEqualTo("CADASTRO");
  }

  @Test
  void usaFuncionalidadeParaExclusaoQuandoBibliotecaNaoTemModeloEspecifico() {
    var hit = AiTemplateSelector.selecionar(
        "Exclusão de registro: localize, clique em excluir e confirme a operação.",
        List.of(
            template("FUNCIONALIDADE", "Guia de funcionalidade"),
            template("INCLUIR_REGISTRO", "Incluir registro"),
            template("EDITAR_REGISTRO", "Editar registro")));

    assertThat(hit.get().getCodigo()).isEqualTo("FUNCIONALIDADE");
  }

  @Test
  void fallbackFuncionalidadeSomenteSemSinal() {
    var padrao = template("FUNCIONALIDADE", "Guia de funcionalidade");
    var hit = AiTemplateSelector.selecionar("texto genérico sem keywords", List.of(padrao));
    assertThat(hit.get().getCodigo()).isEqualTo("FUNCIONALIDADE");
  }

  @Test
  void prefereModeloPersonalizadoPorNomeNoBriefing() {
    var func = template("FUNCIONALIDADE", "Guia de funcionalidade");
    var custom = new PaginaTemplate(
        "PEDIDOS_CONSULTA",
        "Consulta de pedidos",
        "Filtros e exportação de pedidos",
        "<section class=\"doc-intro\"/>",
        1,
        true);
    var hit = AiTemplateSelector.selecionar(
        "Preciso documentar a consulta de pedidos com filtros",
        List.of(func, custom));
    assertThat(hit.get().getCodigo()).isEqualTo("PEDIDOS_CONSULTA");
  }

  @Test
  void naoEscolheFuncionalidadeQuandoHaConsulta() {
    var func = template("FUNCIONALIDADE", "Guia de funcionalidade");
    var consulta = template("CONSULTA", "Consulta");
    var hit = AiTemplateSelector.selecionar(
        "Tela de consulta com filtro e busca",
        List.of(func, consulta));
    assertThat(hit.get().getCodigo()).isEqualTo("CONSULTA");
  }

  @Test
  void recomendaComAltaConfiancaQuandoHaSinalClaro() {
    var recomendacoes = AiTemplateSelector.recomendar(
        "FAQ com dúvidas e perguntas frequentes dos usuários",
        List.of(template("FAQ", "FAQ"), template("PROCESSO", "Processo")));

    assertThat(recomendacoes.get(0).template().getCodigo()).isEqualTo("FAQ");
    assertThat(recomendacoes.get(0).confianca())
        .isGreaterThanOrEqualTo(AiTemplateSelector.CONFIANCA_AUTO_SELECAO);
    assertThat(recomendacoes.get(0).motivo()).isNotBlank();
  }

  @Test
  void exigeConfirmacaoImplicitamenteQuandoNaoHaSinal() {
    var recomendacoes = AiTemplateSelector.recomendar(
        "texto genérico sobre uma página",
        List.of(template("FUNCIONALIDADE", "Funcionalidade"), template("FAQ", "FAQ")));

    assertThat(recomendacoes.get(0).confianca())
        .isLessThan(AiTemplateSelector.CONFIANCA_AUTO_SELECAO);
  }

  @Test
  void preservaEstruturaQuandoClassesMantidas() {
    String esqueleto = "<section class=\"doc-intro\"></section><div class=\"screen-placeholder\"></div>";
    String gerado = "<section class=\"doc-intro\"><p>x</p></section><div class=\"screen-placeholder\"></div>";
    assertThat(AiTemplateSelector.preservaEstrutura(esqueleto, gerado)).isTrue();
  }

  @Test
  void rejeitaQuandoInventaLayout() {
    String esqueleto = "<section class=\"doc-intro\"></section><div class=\"screen-placeholder\"></div><ol class=\"steps\"></ol>";
    String inventado = "<div class=\"meu-layout-novo\"><h1>Outra coisa</h1></div>";
    assertThat(AiTemplateSelector.preservaEstrutura(esqueleto, inventado)).isFalse();
  }

  private static PaginaTemplate template(String codigo, String nome) {
    return new PaginaTemplate(codigo, nome, "desc", "<p/>", 0, true);
  }
}
