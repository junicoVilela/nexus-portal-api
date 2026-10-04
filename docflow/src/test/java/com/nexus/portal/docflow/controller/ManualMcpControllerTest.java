package com.nexus.portal.docflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.service.ManualCorpusService;
import com.nexus.portal.docflow.service.ManualCorpusService.Corpus;
import com.nexus.portal.docflow.service.ManualCorpusService.Documento;
import com.nexus.portal.docflow.service.ManualCorpusService.Secao;
import com.nexus.portal.docflow.service.PreviewTokenService;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManualMcpControllerTest {

  private final ObjectMapper json = new ObjectMapper();
  private final PreviewTokenService tokens = mock(PreviewTokenService.class);
  private final ManualCorpusService corpusService = mock(ManualCorpusService.class);
  private final ManualMcpController controller = new ManualMcpController(tokens, corpusService, json);
  private final UUID cliente = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    Documento doc = new Documento("PED-001", "Consulta de pedidos", "Vendas › Consulta de pedidos",
        "paginas/pedidos.html", "# Consulta de pedidos\n\n## Filtros\n\nFiltre por período e status.");
    Map<String, Documento> docs = new LinkedHashMap<>(Map.of("PED-001", doc));
    when(tokens.clienteDoToken("tok")).thenReturn(cliente);
    when(tokens.clienteDoToken("ruim")).thenThrow(new NotFoundException("Token inválido ou expirado."));
    when(corpusService.vigenteDoCliente(cliente)).thenReturn(new Corpus(UUID.randomUUID(), "ACME", "1.5.0", docs,
        List.of(new Secao(doc, "Filtros", "Filtre por período e status."))));
  }

  private JsonNode chamar(String token, String corpo) throws Exception {
    var resposta = controller.mensagem(token == null ? null : "Bearer " + token, corpo);
    return json.readTree(resposta.getBody());
  }

  @Test
  void inicializaEListaAsFerramentas() throws Exception {
    JsonNode init = chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
    assertThat(init.path("result").path("protocolVersion").asText()).isEqualTo(ManualMcpController.VERSAO_PROTOCOLO);
    assertThat(init.path("result").path("instructions").asText()).contains("Manual ACME v1.5.0");

    JsonNode lista = chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
    assertThat(lista.path("result").path("tools").findValuesAsText("name"))
        .containsExactly("buscar", "paginaPorCodigo", "listarTelas");
  }

  @Test
  void buscarEPaginaPorCodigoUsamOManualVigente() throws Exception {
    JsonNode busca = chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
        + "\"params\":{\"name\":\"buscar\",\"arguments\":{\"consulta\":\"filtrar por status\"}}}");
    assertThat(busca.path("result").path("content").get(0).path("text").asText())
        .contains("Tela: PED-001", "Filtre por período");

    JsonNode pagina = chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
        + "\"params\":{\"name\":\"paginaPorCodigo\",\"arguments\":{\"codigoTela\":\"ped-001\"}}}");
    assertThat(pagina.path("result").path("content").get(0).path("text").asText()).contains("## Filtros");

    JsonNode ausente = chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
        + "\"params\":{\"name\":\"paginaPorCodigo\",\"arguments\":{\"codigoTela\":\"NF-001\"}}}");
    assertThat(ausente.path("result").path("isError").asBoolean()).isTrue();
  }

  @Test
  void buscaSemBaseOrientaANaoInventar() throws Exception {
    JsonNode busca = chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\","
        + "\"params\":{\"name\":\"buscar\",\"arguments\":{\"consulta\":\"emitir nota fiscal\"}}}");
    assertThat(busca.path("result").path("content").get(0).path("text").asText()).contains("Não invente");
  }

  @Test
  void semTokenOuTokenInvalidoResponde401() throws Exception {
    var semToken = controller.mensagem(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");
    assertThat(semToken.getStatusCode().value()).isEqualTo(401);
    var invalido = controller.mensagem("Bearer ruim", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");
    assertThat(invalido.getStatusCode().value()).isEqualTo(401);
    assertThat(json.readTree(invalido.getBody()).path("error").path("code").asInt()).isEqualTo(-32001);
  }

  @Test
  void jsonInvalidoResponde400ComParseError() throws Exception {
    var resposta = controller.mensagem("Bearer tok", "{nao e json");
    assertThat(resposta.getStatusCode().value()).isEqualTo(400);
    assertThat(json.readTree(resposta.getBody()).path("error").path("code").asInt()).isEqualTo(-32700);
  }

  @Test
  void notificacaoRecebe202EMetodoDesconhecidoErro() throws Exception {
    assertThat(controller.mensagem("Bearer tok",
        "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}").getStatusCode().value())
        .isEqualTo(202);
    assertThat(chamar("tok", "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"resources/list\"}")
        .path("error").path("code").asInt()).isEqualTo(-32601);
  }
}
