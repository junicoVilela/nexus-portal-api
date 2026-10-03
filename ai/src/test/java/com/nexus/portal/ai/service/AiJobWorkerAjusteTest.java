package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.provider.FakeLlmProvider;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Worker no modo ajuste de página (Fase B), ponta a ponta com o provider fake. */
@ExtendWith(MockitoExtension.class)
class AiJobWorkerAjusteTest {

  @Mock AiJobRepository jobRepository;
  @Mock AiMensagemRepository mensagemRepository;
  @Mock AiPropostaRepository propostaRepository;
  @Mock DocFlowAiBridge docFlowAiBridge;
  @Mock AiEventService aiEventService;
  @Mock AuditoriaService auditoriaService;

  private static final String HTML = "<h2>Pré-requisitos</h2><p>Ter o perfil Operador.</p>";
  private final UUID paginaId = UUID.randomUUID();
  private AiJobWorkerService worker;
  private AiSessao sessao;
  private AiJob job;

  @BeforeEach
  void setUp() throws Exception {
    AiProperties props = new AiProperties(true, null, null, "fake-model", null, null, null, 30, 5, 2000, 20);
    ObjectMapper objectMapper = new ObjectMapper();
    worker = new AiJobWorkerService(
        mensagemRepository,
        new AiJobLifecycleService(jobRepository, propostaRepository, aiEventService, auditoriaService),
        docFlowAiBridge,
        new FakeLlmProvider(),
        props,
        new AiHtmlSanitizer(),
        new AiTriagemService(props),
        new AiComponenteRetriever(),
        new AiPageSpecService(objectMapper, docFlowAiBridge),
        new AiBriefingPageSpecEnricher(),
        new AiPagePatchService(objectMapper, docFlowAiBridge, new AiHtmlSanitizer()),
        objectMapper);

    sessao = AiSessao.ajuste("Inclua o perfil gestor.", null, null, paginaId, 5L, null);
    setId(sessao, UUID.randomUUID());
    sessao.gerando();
    job = new AiJob(sessao, AiJobTipo.AJUSTAR, 1);
    setId(job, UUID.randomUUID());
    when(jobRepository.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
  }

  private void pagina(long version) {
    when(docFlowAiBridge.buscarPaginaParaAjuste(paginaId)).thenReturn(new DocFlowAiBridge.PaginaAjuste(
        paginaId, null, null, "Consulta", "consulta", "PED-001", "Resumo", HTML, version, "RASCUNHO"));
  }

  @Test
  void geraPropostaDeAtualizacaoComPatchEHtmlResultante() {
    pagina(5);
    when(mensagemRepository.findBySessaoIdOrderByOrdemAsc(sessao.getId())).thenReturn(List.of());
    when(docFlowAiBridge.listarBlocos()).thenReturn(List.of());
    when(docFlowAiBridge.avaliarQualidade(anyString(), anyString(), any(), anyString()))
        .thenReturn(new ResultadoQualidade(true, List.of()));
    when(propostaRepository.findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(any(), any()))
        .thenReturn(Optional.empty());
    when(propostaRepository.save(any(AiProposta.class))).thenAnswer(inv -> inv.getArgument(0));

    worker.processar(job.getId());

    ArgumentCaptor<AiProposta> captor = ArgumentCaptor.forClass(AiProposta.class);
    verify(propostaRepository).save(captor.capture());
    AiProposta proposta = captor.getValue();
    assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCESSO);
    assertThat(proposta.getTipo()).isEqualTo(AiPropostaTipo.ATUALIZACAO);
    assertThat(proposta.getCodigoTela()).isEqualTo("PED-001");
    assertThat(proposta.getPatchJson()).contains("ALTERAR_TEXTO").contains("op1");
    assertThat(proposta.getConteudoHtml()).contains("(ajustado)");
    assertThat(proposta.getPromptVersao()).startsWith("ajustar-pagina@");
  }

  @Test
  void paginaQueMudouDepoisDoPedidoFalhaComMensagemClara() {
    pagina(6);

    worker.processar(job.getId());

    assertThat(job.getStatus()).isEqualTo(AiJobStatus.ERRO);
    assertThat(job.getErroMensagem()).contains("A página mudou desde o pedido");
    verify(propostaRepository, never()).save(any());
  }

  private static void setId(Object alvo, UUID id) throws Exception {
    var campo = alvo.getClass().getDeclaredField("id");
    campo.setAccessible(true);
    campo.set(alvo, id);
  }
}
