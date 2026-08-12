package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintSecaoResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiComponenteRetrieverTest {

  private final AiComponenteRetriever retriever = new AiComponenteRetriever();

  @Test
  void consultaRecebeEstruturaBaseEComponenteLexicalRelevante() {
    List<PaginaBlocoResponse> resultado = retriever.recuperar(
        "CONSULTA",
        "Consulta com filtros, exportação e mensagens de erro",
        List.of(
            bloco("introducao", "Introdução", "Estrutura"),
            bloco("objetivo", "Objetivo", "Orientação"),
            bloco("visao-tela", "Visão da tela", "Estrutura"),
            bloco("filtros-resultado", "Filtros e resultado", "Referência"),
            bloco("acoes-tela", "Ações da tela", "Referência"),
            bloco("resultado-esperado", "Resultado esperado", "Orientação"),
            bloco("mensagens-sistema", "Mensagens de erro do sistema", "Referência"),
            bloco("kit-lista", "Kit", "Kits")),
        blueprintConsulta());

    assertThat(resultado).extracting(PaginaBlocoResponse::id)
        .containsSubsequence("introducao", "objetivo", "visao-tela", "filtros-resultado")
        .contains("mensagens-sistema")
        .doesNotContain("kit-lista");
  }

  private static PaginaBlocoResponse bloco(String id, String nome, String categoria) {
    return new PaginaBlocoResponse(
        id, nome, nome, categoria, "intro", "<p>Texto padrão</p>", null, 1, List.of());
  }

  private static PaginaBlueprintResponse blueprintConsulta() {
    return new PaginaBlueprintResponse(
        "consulta-operacional",
        "Consulta operacional",
        "Consulta com filtros e ações",
        "CONSULTA",
        1,
        "PUBLICADO",
        4,
        7,
        List.of("CONSULTA"),
        List.of(
            secao("abertura", "introducao", "OBRIGATORIA"),
            secao("objetivo", "objetivo", "RECOMENDADA"),
            secao("tela", "visao-tela", "OBRIGATORIA"),
            secao("filtros", "filtros-resultado", "OBRIGATORIA"),
            secao("acoes", "acoes-tela", "RECOMENDADA"),
            secao("falhas", "mensagens-sistema", "OPCIONAL"),
            secao("resultado", "resultado-esperado", "OBRIGATORIA")));
  }

  private static PaginaBlueprintSecaoResponse secao(
      String slot, String componenteId, String necessidade) {
    return new PaginaBlueprintSecaoResponse(
        slot, componenteId, necessidade, false, 1, List.of());
  }
}
