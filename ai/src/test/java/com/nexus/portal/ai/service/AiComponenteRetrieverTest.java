package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
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
            bloco("kit-lista", "Kit", "Kits")));

    assertThat(resultado).extracting(PaginaBlocoResponse::id)
        .containsSubsequence("introducao", "objetivo", "visao-tela", "filtros-resultado")
        .contains("mensagens-sistema")
        .doesNotContain("kit-lista");
  }

  private static PaginaBlocoResponse bloco(String id, String nome, String categoria) {
    return new PaginaBlocoResponse(
        id, nome, nome, categoria, "intro", "<p>Texto padrão</p>", null, 1, List.of());
  }
}
