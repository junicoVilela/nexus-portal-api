package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiManualPergunta;
import com.nexus.portal.ai.entity.AiManualPergunta.Modo;
import com.nexus.portal.ai.entity.AiManualPergunta.Origem;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.provider.LlmCompletion;
import com.nexus.portal.ai.provider.LlmProvider;
import com.nexus.portal.ai.repository.AiManualPerguntaRepository;
import com.nexus.portal.docflow.service.ManualCorpusService;
import com.nexus.portal.docflow.service.ManualCorpusService.Corpus;
import com.nexus.portal.docflow.service.ManualCorpusService.Documento;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiManualPerguntaServiceTest {

  private final LlmProvider llm = mock(LlmProvider.class);
  private final AiManualPerguntaRepository repositorio = mock(AiManualPerguntaRepository.class);

  private AiManualPerguntaService service(boolean iaLigada) {
    AiProperties props = new AiProperties(iaLigada, null, "k", null, null, null, null, 0, 0, 0, 0);
    return new AiManualPerguntaService(mock(DocFlowAiBridge.class), llm, props, repositorio, new ObjectMapper());
  }

  private static Corpus corpus() {
    Map<String, Documento> docs = new LinkedHashMap<>();
    docs.put("PED-001", new Documento("PED-001", "Consulta de pedidos", "Vendas › Consulta de pedidos",
        "paginas/pedidos.html", "# Consulta de pedidos\n\n## Filtros\n\nFiltre os pedidos por período e status."));
    var secoes = docs.values().stream().flatMap(d -> ManualCorpusServiceSecoes.de(d).stream()).toList();
    return new Corpus(UUID.randomUUID(), "ACME", "1.5.0", docs, secoes);
  }

  private void respostaDaIa(String json) {
    when(llm.completarEstruturado(anyString(), anyString(), any())).thenReturn(LlmCompletion.of(json));
  }

  @Test
  void semTrechoQueCubraAPerguntaDizNaoSeiSemChamarAIa() {
    var resposta = service(true).responder(corpus(), "Como emitir nota fiscal?", Origem.LEITOR);

    assertThat(resposta.modo()).isEqualTo(Modo.NAO_SEI);
    assertThat(resposta.citacoes()).isEmpty();
    verify(llm, never()).completarEstruturado(any(), any(), any());
  }

  @Test
  void respostaDaIaSaiComCitacaoValidaEOPromptLevaSoOsTrechos() {
    respostaDaIa("{\"encontrou\":true,\"resposta\":\"Use os filtros (PED-001).\",\"citacoes\":[\"PED-001\",\"XYZ-999\"]}");

    var resposta = service(true).responder(corpus(), "como filtrar pedidos por status", Origem.PORTAL);

    assertThat(resposta.modo()).isEqualTo(Modo.IA);
    assertThat(resposta.resposta()).isEqualTo("Use os filtros (PED-001).");
    assertThat(resposta.citacoes()).extracting(c -> c.codigoTela()).containsExactly("PED-001");
    assertThat(resposta.citacoes().getFirst().url()).isEqualTo("paginas/pedidos.html");
    ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
    verify(llm).completarEstruturado(anyString(), user.capture(), any());
    assertThat(user.getValue()).contains("TAREFA=RESPONDER_MANUAL", "codigoTela: PED-001", "Filtre os pedidos");
  }

  @Test
  void iaSemCitacaoValidaOuQueNaoEncontrouViraNaoSei() {
    respostaDaIa("{\"encontrou\":true,\"resposta\":\"Inventado.\",\"citacoes\":[\"XYZ-999\"]}");
    assertThat(service(true).responder(corpus(), "filtrar pedidos", Origem.LEITOR).modo()).isEqualTo(Modo.NAO_SEI);

    respostaDaIa("{\"encontrou\":false,\"resposta\":\"\",\"citacoes\":[]}");
    assertThat(service(true).responder(corpus(), "filtrar pedidos", Origem.LEITOR).modo()).isEqualTo(Modo.NAO_SEI);
  }

  @Test
  void falhaDaIaOuIaDesligadaDevolveOsTrechos() {
    when(llm.completarEstruturado(anyString(), anyString(), any())).thenThrow(new IllegalStateException("timeout"));
    var comFalha = service(true).responder(corpus(), "filtrar pedidos", Origem.LEITOR);
    assertThat(comFalha.modo()).isEqualTo(Modo.TRECHOS);
    assertThat(comFalha.citacoes()).extracting(c -> c.secao()).containsExactly("Filtros");

    var desligada = service(false).responder(corpus(), "filtrar pedidos", Origem.LEITOR);
    assertThat(desligada.modo()).isEqualTo(Modo.TRECHOS);
  }

  @Test
  void registraAPerguntaSemIdentificarQuemPerguntou() {
    service(false).responder(corpus(), "  filtrar pedidos  ", Origem.LEITOR);

    ArgumentCaptor<AiManualPergunta> salva = ArgumentCaptor.forClass(AiManualPergunta.class);
    verify(repositorio).save(salva.capture());
    assertThat(salva.getValue().getPergunta()).isEqualTo("filtrar pedidos");
    assertThat(salva.getValue().getOrigem()).isEqualTo(Origem.LEITOR);
    assertThat(salva.getValue().getCodigosCitados()).containsExactly("PED-001");
  }

  /** {@code ManualCorpusService.secoes} é package-private no docflow. */
  private static final class ManualCorpusServiceSecoes {
    static List<ManualCorpusService.Secao> de(Documento doc) {
      return List.of(new ManualCorpusService.Secao(doc, "Filtros", "Filtre os pedidos por período e status."));
    }
  }
}
