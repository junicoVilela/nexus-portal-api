package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.provider.FakeLlmProvider;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.docflow.service.PaginaQualidadeService;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ItemQualidade;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import com.nexus.portal.docflow.service.PaginaQualidadeService.Severidade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobWorkerServiceTest {

  @Mock AiJobRepository jobRepository;
  @Mock AiSessaoRepository sessaoRepository;
  @Mock AiMensagemRepository mensagemRepository;
  @Mock AiPropostaRepository propostaRepository;
  @Mock DocFlowAiBridge docFlowAiBridge;
  @Mock AiEventService aiEventService;
  @Mock AuditoriaService auditoriaService;

  private AiJobWorkerService worker;
  private AiSessao sessao;
  private AiJob job;

  @BeforeEach
  void setUp() throws Exception {
    AiProperties props = new AiProperties(true, null, null, "fake-model", null, null, 30, 5, 2000, 20);
    worker = new AiJobWorkerService(
        jobRepository,
        sessaoRepository,
        mensagemRepository,
        propostaRepository,
        docFlowAiBridge,
        new FakeLlmProvider(),
        props,
        new AiHtmlSanitizer(),
        aiEventService,
        new AiTriagemService(props),
        new ObjectMapper(),
        auditoriaService);

    sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        """
            Consulta de pedidos
            codigoTela: PED-CONSULTA
            Fluxo completo para filtrar e exportar pedidos do cliente no portal.
            """,
        null, null, null, null, null);
    setId(sessao, UUID.randomUUID());
    sessao.gerando();

    job = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO);
    setJobId(job, UUID.randomUUID());

    when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    when(mensagemRepository.findBySessaoIdOrderByOrdemAsc(sessao.getId())).thenReturn(List.of());
    when(docFlowAiBridge.buscarTemplate(any(), any(), any())).thenReturn(Optional.empty());
    when(docFlowAiBridge.avaliarQualidade(anyString(), anyString(), any(), anyString()))
        .thenReturn(new ResultadoQualidade(true, List.of(
            new ItemQualidade("TITULO", "Título", "ok", true, Severidade.ERRO))));
    when(propostaRepository.findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(any(), any()))
        .thenReturn(Optional.empty());
    when(propostaRepository.save(any(AiProposta.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void processarGeraPropostaComFakeLlm() {
    worker.processar(job.getId());

    ArgumentCaptor<AiProposta> captor = ArgumentCaptor.forClass(AiProposta.class);
    verify(propostaRepository).save(captor.capture());
    AiProposta proposta = captor.getValue();

    assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCESSO);
    assertThat(sessao.getStatus()).isEqualTo(AiSessaoStatus.PRONTA);
    assertThat(proposta.getTitulo()).isNotBlank();
    assertThat(proposta.getConteudoHtml()).contains("doc-intro");
    assertThat(proposta.getCodigoTela()).isEqualTo("PED-CONSULTA");
    verify(aiEventService).publicarJob(eq(job.getId()), eq(sessao.getId()), eq("SUCESSO"), eq(100));
  }

  private static void setId(AiSessao sessao, UUID id) throws Exception {
    var field = AiSessao.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(sessao, id);
  }

  private static void setJobId(AiJob job, UUID id) throws Exception {
    var field = AiJob.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(job, id);
  }
}
