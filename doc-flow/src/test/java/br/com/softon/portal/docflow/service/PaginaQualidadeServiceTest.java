package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.Projeto;
import org.junit.jupiter.api.Test;

class PaginaQualidadeServiceTest {

  private final PaginaQualidadeService service = new PaginaQualidadeService();

  @Test
  void avaliar_comConteudoFinalizado_deveEstarAptoParaRevisao() {
    Pagina pagina = pagina(
        "<h2>Como cadastrar</h2><p>Acesse a tela de clientes, preencha os dados obrigatórios, "
            + "revise as informações apresentadas e selecione Salvar para concluir o cadastro com segurança.</p>",
        "Orientações completas para realizar o cadastro de clientes no sistema.");

    var resultado = service.avaliar(pagina);

    assertThat(resultado.aptoParaRevisao()).isTrue();
    assertThat(resultado.itens()).allMatch(item -> item.severidade() != PaginaQualidadeService.Severidade.ERRO
        || item.ok());
  }

  @Test
  void avaliar_comPlaceholderEImagemSemAlt_deveApontarPendencias() {
    Pagina pagina = pagina(
        "<h2>Como cadastrar</h2><p>Explique como o usuário deve preencher esta tela.</p><img src=\"tela.png\">",
        "Resumo curto");

    var resultado = service.avaliar(pagina);

    assertThat(resultado.aptoParaRevisao()).isFalse();
    assertThat(resultado.itens()).filteredOn(item -> !item.ok())
        .extracting(PaginaQualidadeService.ItemQualidade::codigo)
        .contains("CONTEUDO", "PLACEHOLDERS", "IMAGENS_ALT", "RESUMO");
  }

  @Test
  void avaliar_comLinkInvalidoEHierarquiaQuebrada_deveApontarPendencias() {
    Pagina pagina = pagina(
        "<h2>Orientações</h2><h4>Detalhes</h4><p>Consulte as instruções completas para executar "
            + "o processo com segurança e validar todos os dados necessários.</p><a href=\"javascript:void(0)\">Abrir</a>",
        "Orientações completas para realizar o cadastro de clientes no sistema.");

    var resultado = service.avaliar(pagina);

    assertThat(resultado.itens()).filteredOn(item -> !item.ok())
        .extracting(PaginaQualidadeService.ItemQualidade::codigo)
        .contains("LINKS", "TITULOS");
  }

  @Test
  void avaliar_comPlaceholderMustache_deveApontarPendencia() {
    Pagina pagina = pagina(
        "<h2>Campo</h2><p>O valor padrão é {{campo.nome}} e deve ser validado antes de salvar o registro no sistema.</p>",
        "Orientações completas para realizar o cadastro de clientes no sistema.");

    var resultado = service.avaliar(pagina);

    assertThat(resultado.aptoParaRevisao()).isFalse();
    assertThat(resultado.itens()).filteredOn(item -> "PLACEHOLDERS".equals(item.codigo()))
        .singleElement()
        .satisfies(item -> {
          assertThat(item.ok()).isFalse();
          assertThat(item.descricao()).contains("{{campo.nome}}");
        });
  }

  @Test
  void avaliar_comScreenPlaceholderSemImagem_deveApontarAvisoCaptura() {
    Pagina pagina = pagina(
        "<h2>Tela</h2><p>Visualize a área principal da tela antes de executar qualquer alteração nos dados do registro.</p>"
            + "<div class=\"screen-placeholder\">Área reservada para captura.</div>",
        "Orientações completas para realizar o cadastro de clientes no sistema.");

    var resultado = service.avaliar(pagina);

    assertThat(resultado.itens()).filteredOn(item -> "CAPTURA".equals(item.codigo()))
        .singleElement()
        .satisfies(item -> {
          assertThat(item.ok()).isFalse();
          assertThat(item.severidade()).isEqualTo(PaginaQualidadeService.Severidade.AVISO);
        });
  }

  private Pagina pagina(String conteudo, String resumo) {
    Projeto projeto = new Projeto("Projeto", "projeto", null, true);
    Modulo modulo = new Modulo("Módulo", "modulo", null, 0, true, projeto);
    return new Pagina("Cadastro de clientes", "cadastro-clientes", "CLI-001", resumo,
        conteudo, 0, true, modulo, null);
  }
}
