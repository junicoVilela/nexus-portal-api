package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoSlotResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintSecaoResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPageSpecVersioningTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void registraVersaoDoContratoEBlueprintDeOrigem() throws Exception {
    var componente = new PaginaBlocoResponse(
        "introducao",
        "Introdução",
        "Contextualiza a página.",
        "Estrutura",
        "intro",
        "<section><h2>Título</h2></section>",
        null,
        1,
        List.of(new PaginaBlocoSlotResponse("t1", "h2", "", "Título")));
    var blueprint = new PaginaBlueprintResponse(
        "funcionalidade-geral",
        "Funcionalidade geral",
        "Estrutura editorial",
        "FUNCIONALIDADE",
        1,
        "PUBLICADO",
        1,
        3,
        List.of("FUNCIONALIDADE"),
        List.of(new PaginaBlueprintSecaoResponse(
            "abertura", "introducao", "OBRIGATORIA", false, 1, List.of())));
    var json = objectMapper.readTree("""
        {
          "titulo":"Consulta",
          "slug":"consulta",
          "codigoTela":"PED-001",
          "resumo":"Consulte pedidos.",
          "blocos":[{
            "componenteId":"introducao",
            "textos":[{"slotId":"t1","valor":"Consulta de pedidos"}]
          }]
        }
        """);
    var service = new AiPageSpecService(objectMapper, null);

    AiPageSpec spec = service.interpretar(json, List.of(componente), blueprint);

    assertThat(spec.schemaVersion()).isEqualTo(2);
    assertThat(spec.blueprintId()).isEqualTo("funcionalidade-geral");
  }
}
