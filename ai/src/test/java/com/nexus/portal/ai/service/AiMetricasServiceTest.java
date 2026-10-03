package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiMetricasServiceTest {

  private final AiJobRepository jobRepository = mock(AiJobRepository.class);
  private final AiPropostaRepository propostaRepository = mock(AiPropostaRepository.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AiPagePatchService patchService =
      new AiPagePatchService(objectMapper, mock(DocFlowAiBridge.class), new AiHtmlSanitizer());
  private AiMetricasService service;
  private AiSessao sessao;
  private AiJob job;

  @BeforeEach
  void setUp() {
    service = new AiMetricasService(jobRepository, propostaRepository, patchService);
    sessao = new AiSessao(AiObjetivo.CRIAR_PAGINA, "briefing", null, null, null, null, null);
    job = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, 1);
  }

  private AiProposta proposta(String prompt, List<String> avisos, String patchJson) {
    return new AiProposta(sessao, job, patchJson == null ? AiPropostaTipo.NOVA : AiPropostaTipo.ATUALIZACAO,
        "t", "t", "C", null, "<p/>", null, null, null, null, avisos, prompt, patchJson);
  }

  @Test
  void taxaDeAceitePorPromptContaRegeneracaoComoNaoServiu() {
    AiProposta aceita = proposta("gerar-page-spec@2.2", List.of(), null);
    aceita.aceitar(UUID.randomUUID());
    AiProposta rejeitada = proposta("gerar-page-spec@2.2", List.of("A IA devolveu uma resposta inválida."), null);
    rejeitada.rejeitar("Texto genérico");
    AiProposta regenerada = proposta("gerar-page-spec@2.2", List.of("A IA devolveu uma resposta inválida."), null);
    regenerada.descartar();
    AiProposta pendente = proposta("gerar-page-spec@2.2", List.of(), null);
    AiProposta antiga = proposta(null, List.of(), null);
    when(propostaRepository.findByCreatedAtAfter(any()))
        .thenReturn(List.of(aceita, rejeitada, regenerada, pendente, antiga));
    when(jobRepository.findByCreatedAtAfter(any())).thenReturn(List.of());

    var metricas = service.calcular(30);

    var atual = metricas.porPrompt().stream()
        .filter(p -> p.promptVersao().equals("gerar-page-spec@2.2")).findFirst().orElseThrow();
    assertThat(atual.propostas()).isEqualTo(4);
    assertThat(atual.pendentes()).isEqualTo(1);
    assertThat(atual.comAvisos()).isEqualTo(2);
    assertThat(atual.taxaAceite()).isEqualTo(1.0 / 3);
    assertThat(metricas.porPrompt()).anyMatch(p -> p.promptVersao().equals(AiMetricasService.SEM_VERSAO));
    assertThat(metricas.avisosFrequentes().getFirst().ocorrencias()).isEqualTo(2);
    assertThat(metricas.rejeicoesRecentes()).extracting(r -> r.motivo()).containsExactly("Texto genérico");
  }

  @Test
  void aceiteParcialDosAjustesPorTipoDeOperacao() throws Exception {
    AiPagePatch patch = new AiPagePatch("r", List.of(
        new AiPagePatch.Operacao("op1", AiPagePatch.Tipo.ALTERAR_TEXTO, "u1", "a", "b", null, null, null, "m"),
        new AiPagePatch.Operacao("op2", AiPagePatch.Tipo.ALTERAR_TEXTO, "u2", "a", "b", null, null, null, "m"),
        new AiPagePatch.Operacao("op3", AiPagePatch.Tipo.REMOVER_UNIDADE, "u3", "a", null, null, null, null, "m")));
    AiProposta ajuste = proposta("ajustar-pagina@1.1", List.of(), objectMapper.writeValueAsString(patch));
    ajuste.aceitarAjuste(UUID.randomUUID(), List.of("op1", "op2"));
    when(propostaRepository.findByCreatedAtAfter(any())).thenReturn(List.of(ajuste));
    when(jobRepository.findByCreatedAtAfter(any())).thenReturn(List.of());

    var ajustes = service.calcular(7).ajustes();

    assertThat(ajustes.aplicados()).isEqualTo(1);
    assertThat(ajustes.operacoesPropostas()).isEqualTo(3);
    assertThat(ajustes.operacoesAceitas()).isEqualTo(2);
    assertThat(ajustes.porTipo()).anySatisfy(t -> {
      assertThat(t.tipo()).isEqualTo("REMOVER_UNIDADE");
      assertThat(t.propostas()).isEqualTo(1);
      assertThat(t.aceitas()).isZero();
    });
  }

  @Test
  void geracaoSomaTokensEContaStatus() {
    AiJob sucesso = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, 1);
    sucesso.iniciar("m");
    sucesso.registrarTokens(100, 50);
    sucesso.sucesso();
    AiJob erro = new AiJob(sessao, AiJobTipo.AJUSTAR, 1);
    erro.iniciar("m");
    erro.erro("falhou", "detalhe", UUID.randomUUID());
    when(jobRepository.findByCreatedAtAfter(any())).thenReturn(List.of(sucesso, erro));
    when(propostaRepository.findByCreatedAtAfter(any())).thenReturn(List.of());

    var geracao = service.calcular(30).geracao();

    assertThat(geracao.jobs()).isEqualTo(2);
    assertThat(geracao.sucesso()).isEqualTo(1);
    assertThat(geracao.erro()).isEqualTo(1);
    assertThat(geracao.tokensEntrada()).isEqualTo(100);
    assertThat(geracao.tokensSaida()).isEqualTo(50);
  }

  @Test
  void percentilPeloValorMaisProximo() {
    assertThat(AiMetricasService.percentil(List.of(), 50)).isNull();
    assertThat(AiMetricasService.percentil(List.of(10L, 20L, 30L, 40L), 50)).isEqualTo(20L);
    assertThat(AiMetricasService.percentil(List.of(10L, 20L, 30L, 40L), 90)).isEqualTo(40L);
  }

  @Test
  void periodoForaDoLimiteEhRecusado() {
    assertThatThrownBy(() -> service.calcular(0)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.calcular(400)).isInstanceOf(BusinessException.class);
  }
}
