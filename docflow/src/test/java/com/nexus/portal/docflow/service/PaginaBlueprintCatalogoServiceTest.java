package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaBlueprintCatalogoServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PaginaBlocoCatalogoService blocoService =
      new PaginaBlocoCatalogoService(objectMapper);
  private final PaginaBlueprintCatalogoService service =
      new PaginaBlueprintCatalogoService(objectMapper, blocoService);

  @Test
  void carregaBlueprintsComComposicaoValidaEIdsUnicos() {
    var blueprints = service.listar();

    assertThat(blueprints).hasSize(10);
    assertThat(blueprints).extracting(blueprint -> blueprint.id()).doesNotHaveDuplicates();
    assertThat(blueprints)
        .flatExtracting(blueprint -> blueprint.secoes())
        .extracting(secao -> secao.componenteId())
        .allMatch(id -> blocoService.listar().stream().anyMatch(bloco -> bloco.id().equals(id)));
  }

  @Test
  void resolveTodosOsTemplatesDeSistemaParaUmaReceitaEditorial() {
    List<String> templates = List.of(
        "CADASTRO",
        "CATALOGO_PARAMETROS",
        "CATEGORIA_ARTIGOS",
        "CENTRAL_AJUDA",
        "CONSULTA",
        "DICIONARIO_CAMPOS",
        "DOSSIE_DECISAO",
        "EDITAR_REGISTRO",
        "ESPECIFICACAO_REGRA",
        "FAQ",
        "FUNCIONALIDADE",
        "INCLUIR_REGISTRO",
        "LAB_FILTROS",
        "LISTAR_REGISTROS",
        "PAINEL_METRICAS",
        "PASSO_A_PASSO",
        "PRIMEIROS_PASSOS",
        "PROCESSO",
        "RELATORIO",
        "SOLUCAO_PROBLEMAS");

    assertThat(templates)
        .allMatch(template -> service.buscarPorTemplate(template).isPresent());
  }

  @Test
  void blueprintDeConsultaExplicitaObrigatoriasRecomendadasEOpcionais() {
    var blueprint = service.buscarPorTemplate("consulta").orElseThrow();

    assertThat(blueprint.id()).isEqualTo("consulta-operacional");
    assertThat(blueprint.secoes())
        .extracting(secao -> secao.necessidade())
        .contains("OBRIGATORIA", "RECOMENDADA", "OPCIONAL");
    assertThat(blueprint.secoes())
        .extracting(secao -> secao.componenteId())
        .containsSubsequence("introducao", "objetivo", "pre-requisitos", "visao-tela");
  }
}
