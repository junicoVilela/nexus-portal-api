package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoSlotResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiPageSpecServiceTest {

  @Mock DocFlowAiBridge bridge;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void validaSlotsERenderizaSomentePeloBridge() throws Exception {
    AiPageSpecService service = new AiPageSpecService(objectMapper, bridge);
    var componente = componente();
    var json = objectMapper.readTree("""
        {
          "titulo":"Consulta",
          "slug":"consulta",
          "codigoTela":"PED-001",
          "resumo":"Consulte pedidos.",
          "blocos":[{
            "componenteId":"introducao",
            "textos":[{"slotId":"t2","valor":"Consulta de pedidos"}]
          }]
        }
        """);
    when(bridge.renderizarBloco("introducao", Map.of("t2", "Consulta de pedidos")))
        .thenReturn("<section class=\"doc-intro\"><h2>Consulta de pedidos</h2></section>");

    AiPageSpec spec = service.interpretar(json, List.of(componente));
    String html = service.renderizar(spec);

    assertThat(html).contains("doc-intro").contains("Consulta de pedidos");
  }

  @Test
  void rejeitaComponenteOuSlotForaDoCatalogoRecuperado() throws Exception {
    AiPageSpecService service = new AiPageSpecService(objectMapper, bridge);
    var json = objectMapper.readTree("""
        {
          "titulo":"Consulta",
          "slug":"consulta",
          "codigoTela":"PED-001",
          "resumo":"Consulte pedidos.",
          "blocos":[{
            "componenteId":"introducao",
            "textos":[{"slotId":"script","valor":"inválido"}]
          }]
        }
        """);

    assertThatThrownBy(() -> service.interpretar(json, List.of(componente())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Slot não permitido");
  }

  @Test
  void schemaRestringeIdsAosCandidatos() {
    AiPageSpecService service = new AiPageSpecService(objectMapper, bridge);

    assertThat(service.schema(List.of(componente())).toString())
        .contains("\"enum\":[\"introducao\"]")
        .contains("\"additionalProperties\":false");
  }

  private static PaginaBlocoResponse componente() {
    return new PaginaBlocoResponse(
        "introducao",
        "Introdução",
        "Contexto",
        "Estrutura",
        "intro",
        "<section><span>Visão geral</span><h2>Título</h2><p>Texto.</p></section>",
        null,
        1,
        List.of(
            new PaginaBlocoSlotResponse("t1", "span", "", "Visão geral"),
            new PaginaBlocoSlotResponse("t2", "h2", "", "Título"),
            new PaginaBlocoSlotResponse("t3", "p", "", "Texto.")));
  }
}
