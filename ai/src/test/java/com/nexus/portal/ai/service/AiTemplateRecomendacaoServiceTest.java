package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.integration.docflow.AiTemplateSelector;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintSecaoResponse;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiTemplateRecomendacaoServiceTest {

  @Mock DocFlowAiBridge bridge;

  private final AiComponenteRetriever retriever = new AiComponenteRetriever();

  @Test
  void recomendaKitCuradoExplicavelSemEntregarTodaBiblioteca() {
    var template = template();
    List<PaginaBlocoResponse> biblioteca = biblioteca();
    when(bridge.recomendarTemplates(any(), any(), any())).thenReturn(List.of(
        new AiTemplateSelector.Recomendacao(template, 0.91, "Consulta identificada.")));
    when(bridge.buscarBlueprint("CONSULTA")).thenReturn(Optional.of(blueprint()));
    when(bridge.listarBlocos()).thenReturn(biblioteca);
    var service = new AiTemplateRecomendacaoService(bridge, retriever);

    var resposta = service.recomendar(new AiTemplateRecomendacaoRequest(
        "Consulta de pedidos com filtros e mensagens de erro.", null, null));

    assertThat(resposta.totalBiblioteca()).isEqualTo(6);
    assertThat(resposta.componentes()).extracting(item -> item.id())
        .contains("introducao", "visao-tela", "filtros-resultado", "mensagens-sistema")
        .doesNotContain("kit-lista");
    assertThat(resposta.componentes())
        .filteredOn(item -> item.id().equals("introducao"))
        .allMatch(item -> item.obrigatorio() && item.motivo().contains("Essencial"));
  }

  @Test
  void validaEssenciaisEPreservaAOrdemAprovadaPeloUsuario() {
    var template = template();
    List<PaginaBlocoResponse> biblioteca = biblioteca();
    when(bridge.buscarTemplate(any(), any(), any(), any())).thenReturn(Optional.of(template));
    when(bridge.buscarBlueprint("CONSULTA")).thenReturn(Optional.of(blueprint()));
    when(bridge.listarBlocos()).thenReturn(biblioteca);
    var service = new AiTemplateRecomendacaoService(bridge, retriever);
    List<String> ordemAprovada = List.of("filtros-resultado", "introducao", "visao-tela");

    assertThat(service.validarComponentes(
        "Consulta com filtros.", null, null, template.getId(), ordemAprovada))
        .containsExactlyElementsOf(ordemAprovada);
    assertThatThrownBy(() -> service.validarComponentes(
        "Consulta com filtros.",
        null,
        null,
        template.getId(),
        List.of("filtros-resultado", "mensagens-sistema", "objetivo")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("essenciais");
  }

  private static PaginaTemplate template() {
    PaginaTemplate template = new PaginaTemplate(
        "CONSULTA", "Consulta", "Consulta operacional", "<section></section>", 1, true);
    try {
      Field id = PaginaTemplate.class.getDeclaredField("id");
      id.setAccessible(true);
      id.set(template, UUID.randomUUID());
      return template;
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static List<PaginaBlocoResponse> biblioteca() {
    return List.of(
        bloco("introducao", "Introdução", "Estrutura"),
        bloco("objetivo", "Objetivo", "Orientação"),
        bloco("visao-tela", "Visão da tela", "Estrutura"),
        bloco("filtros-resultado", "Filtros e resultados", "Referência"),
        bloco("mensagens-sistema", "Mensagens de erro do sistema", "Referência"),
        bloco("kit-lista", "Kit de lista", "Kits"));
  }

  private static PaginaBlocoResponse bloco(String id, String nome, String categoria) {
    return new PaginaBlocoResponse(
        id, nome, nome, categoria, "intro", "<p>Texto padrão</p>", null, 1, List.of());
  }

  private static PaginaBlueprintResponse blueprint() {
    return new PaginaBlueprintResponse(
        "consulta-operacional",
        "Consulta operacional",
        "Consulta com filtros",
        "CONSULTA",
        1,
        "PUBLICADO",
        3,
        6,
        List.of("CONSULTA"),
        List.of(
            secao("abertura", "introducao", "OBRIGATORIA"),
            secao("objetivo", "objetivo", "RECOMENDADA"),
            secao("tela", "visao-tela", "OBRIGATORIA"),
            secao("filtros", "filtros-resultado", "RECOMENDADA"),
            secao("falhas", "mensagens-sistema", "OPCIONAL")));
  }

  private static PaginaBlueprintSecaoResponse secao(
      String slot, String componenteId, String necessidade) {
    return new PaginaBlueprintSecaoResponse(
        slot, componenteId, necessidade, false, 1, List.of());
  }
}
