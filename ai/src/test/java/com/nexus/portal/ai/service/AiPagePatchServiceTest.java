package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoSlotResponse;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiPagePatchServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DocFlowAiBridge bridge = mock(DocFlowAiBridge.class);
  private final List<PaginaBlocoResponse> catalogo = List.of(new PaginaBlocoResponse(
      "callout-atencao", "Atenção", "Aviso", "Conteúdo", "callout", "<aside class=\"callout\"><p>x</p></aside>",
      null, 1, List.of(new PaginaBlocoSlotResponse("t1", "p", null, "x"))));
  private AiPagePatchService service;
  private AiPaginaEsboco esboco;

  @BeforeEach
  void setUp() {
    service = new AiPagePatchService(objectMapper, bridge, new AiHtmlSanitizer());
    esboco = AiPaginaEsboco.de(AiPaginaEsbocoTest.HTML);
    when(bridge.renderizarBloco(eq("callout-atencao"), any())).thenAnswer(inv ->
        "<aside class=\"callout\"><p>" + inv.<java.util.Map<String, String>>getArgument(1).get("t1") + "</p></aside>");
  }

  private JsonNode json(String texto) throws Exception {
    return objectMapper.readTree(texto);
  }

  @Test
  void validaOperacoesEDescartaAsInvalidasComAviso() throws Exception {
    var interpretacao = service.interpretar(json("""
        {"resumoDaMudanca":"ajuste","operacoes":[
          {"tipo":"ALTERAR_TEXTO","unidadeId":"u4","novoTexto":"Ter o perfil Gestor.","motivo":"pedido"},
          {"tipo":"ALTERAR_TEXTO","unidadeId":"u5","novoTexto":"sem link","motivo":"protegida"},
          {"tipo":"ALTERAR_TEXTO","unidadeId":"u99","novoTexto":"x","motivo":"inexistente"},
          {"tipo":"REMOVER_UNIDADE","unidadeId":"u3","motivo":"titulo nao sai"},
          {"tipo":"INSERIR_BLOCO","aposSecaoId":"s3","componenteId":"nao-existe","motivo":"fora do catalogo"},
          {"tipo":"INSERIR_BLOCO","aposSecaoId":"s3","componenteId":"callout-atencao",
           "textos":[{"slotId":"t1","valor":"Limite de 5.000 linhas."}],"motivo":"aviso"}
        ]}"""), esboco, "Consulta", "Resumo", null, catalogo);

    assertThat(interpretacao.patch().operacoes()).extracting(AiPagePatch.Operacao::id).containsExactly("op1", "op2");
    assertThat(interpretacao.patch().operacoes().get(0).textoAntes()).isEqualTo("Ter o perfil Operador.");
    assertThat(interpretacao.avisos()).anyMatch(aviso -> aviso.startsWith("4 mudanças"));
  }

  @Test
  void escopoDeSecaoBloqueiaMudancasForaDela() throws Exception {
    var interpretacao = service.interpretar(json("""
        {"resumoDaMudanca":"","operacoes":[
          {"tipo":"ALTERAR_TEXTO","unidadeId":"u2","novoTexto":"outro","motivo":"fora"},
          {"tipo":"ALTERAR_TEXTO","unidadeId":"u4","novoTexto":"dentro","motivo":"ok"}
        ]}"""), esboco, "Consulta", null, "s2", catalogo);

    assertThat(interpretacao.patch().operacoes()).extracting(AiPagePatch.Operacao::unidadeId).containsExactly("u4");
  }

  @Test
  void respostaInvalidaViraPropostaVaziaComAviso() {
    var interpretacao = service.interpretar(objectMapper.createObjectNode(), esboco, "t", null, null, catalogo);
    assertThat(interpretacao.patch().operacoes()).isEmpty();
    assertThat(interpretacao.avisos()).isNotEmpty();
  }

  @Test
  void aplicaSoAsAceitasPreservandoImagensEFormatacao() throws Exception {
    AiPagePatch patch = service.interpretar(json("""
        {"resumoDaMudanca":"","operacoes":[
          {"tipo":"ALTERAR_TEXTO","unidadeId":"u4","novoTexto":"Ter o perfil Gestor.","motivo":"a"},
          {"tipo":"ALTERAR_TEXTO","unidadeId":"titulo","novoTexto":"Consulta e exportação","motivo":"b"},
          {"tipo":"INSERIR_BLOCO","aposSecaoId":"s2","componenteId":"callout-atencao",
           "textos":[{"slotId":"t1","valor":"Limite de 5.000 linhas."}],"motivo":"c"},
          {"tipo":"REMOVER_UNIDADE","unidadeId":"u10","motivo":"d"}
        ]}"""), esboco, "Consulta", "Resumo", null, catalogo).patch();

    var resultado = service.aplicar(AiPaginaEsbocoTest.HTML, "Consulta", "Resumo", patch, Set.of("op1", "op2", "op3"));

    assertThat(resultado.titulo()).isEqualTo("Consulta e exportação");
    assertThat(resultado.html())
        .contains("Ter o perfil Gestor.")
        .doesNotContain("Ter o perfil Operador.")
        .contains("<a href=\"/vendas\">módulo Vendas</a>")
        .contains("src=\"/api/v1/paginas/1/anexos/2/download\"")
        .contains("Texto solto final.");
    int fimLista = resultado.html().indexOf("</ul>");
    int aviso = resultado.html().indexOf("Limite de 5.000 linhas.");
    int passo = resultado.html().indexOf("<h2>Passo a passo</h2>");
    assertThat(aviso).isBetween(fimLista, passo);
  }

  @Test
  void patchSobreviveAIdaEVoltaEmJson() throws Exception {
    AiPagePatch patch = service.interpretar(json("""
        {"resumoDaMudanca":"r","operacoes":[{"tipo":"REMOVER_UNIDADE","unidadeId":"u10","motivo":"d"}]}"""),
        esboco, "t", null, null, catalogo).patch();

    AiPagePatch lido = service.ler(service.escrever(patch));

    assertThat(lido).isEqualTo(patch);
    assertThat(service.aplicar(AiPaginaEsbocoTest.HTML, "t", null, lido, null).html())
        .doesNotContain("Texto solto final.");
  }
}
