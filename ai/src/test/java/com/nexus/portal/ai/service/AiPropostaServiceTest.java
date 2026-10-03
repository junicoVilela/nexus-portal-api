package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.shared.exception.NotFoundException;
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
class AiPropostaServiceTest {

  @Mock AiSessaoRepository sessaoRepository;
  @Mock AiJobRepository jobRepository;
  @Mock AiPropostaRepository propostaRepository;
  @Mock AiJobWorkerService workerService;
  @Mock AiJobLifecycleService lifecycleService;
  @Mock AiEventService eventService;
  @Mock DocFlowAiBridge docFlowAiBridge;
  @Mock AiRateLimitService rateLimitService;
  @Mock AuditoriaService auditoriaService;
  @Mock AiMensagemRepository mensagemRepository;

  private AiPropostaService service;

  @BeforeEach
  void setUp() {
    AiProperties properties = new AiProperties(
        true, null, "sk-test", "modelo-teste", null, null, null, 90, 5, 1000, 20);
    service = new AiPropostaService(
        sessaoRepository,
        jobRepository,
        propostaRepository,
        workerService,
        lifecycleService,
        eventService,
        properties,
        docFlowAiBridge,
        new ObjectMapper(),
        rateLimitService,
        auditoriaService,
        mensagemRepository);
  }

  @Test
  void cliqueRepetidoRetornaMesmoJobSemConsumirNovaGeracao() throws Exception {
    AiSessao sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        "Briefing completo para uma página operacional.",
        null, null, null, null, null);
    UUID sessaoId = UUID.randomUUID();
    setId(sessao, sessaoId);
    sessao.setCreatedBy("system");
    sessao.gerando();
    AiJob job = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, 1);
    setId(job, UUID.randomUUID());

    when(sessaoRepository.findByIdForUpdate(sessaoId)).thenReturn(Optional.of(sessao));
    when(jobRepository.findFirstBySessaoIdAndStatusInOrderByCreatedAtDesc(
        sessaoId, AiJobLifecycleService.statusAtivos())).thenReturn(Optional.of(job));

    var response = service.gerar(sessaoId, null);

    assertThat(response.id()).isEqualTo(job.getId());
    verify(rateLimitService, never()).exigirGeracaoPermitida(null);
    verify(workerService, never()).processar(job.getId());
  }

  @Test
  void gerarComInstrucaoRegistraPedidoDoAutorNoHistorico() throws Exception {
    AiSessao sessao = sessaoDoUsuario();
    sessao.pronta();
    UUID sessaoId = sessao.getId();
    when(sessaoRepository.findByIdForUpdate(sessaoId)).thenReturn(Optional.of(sessao));
    when(jobRepository.findFirstBySessaoIdAndStatusInOrderByCreatedAtDesc(
        sessaoId, AiJobLifecycleService.statusAtivos())).thenReturn(Optional.empty());
    when(jobRepository.saveAndFlush(any(AiJob.class))).thenAnswer(inv -> {
      AiJob job = inv.getArgument(0);
      setId(job, UUID.randomUUID());
      return job;
    });

    service.gerar(sessaoId, "  Deixe mais curto e foque na exportação.  ", null);

    ArgumentCaptor<AiMensagem> captor = ArgumentCaptor.forClass(AiMensagem.class);
    verify(mensagemRepository).save(captor.capture());
    assertThat(captor.getValue().getPapel()).isEqualTo(AiPapelMensagem.USUARIO);
    assertThat(captor.getValue().getConteudo()).isEqualTo("Deixe mais curto e foque na exportação.");
    assertThat(AiPayloadJson.lerInstrucao(new ObjectMapper(), captor.getValue().getPayloadJson()))
        .isEqualTo("Deixe mais curto e foque na exportação.");
    assertThat(sessao.getStatus()).isEqualTo(AiSessaoStatus.GERANDO);
  }

  @Test
  void rejeitarMarcaPropostaPendenteComMotivo() throws Exception {
    AiSessao sessao = sessaoDoUsuario();
    UUID sessaoId = sessao.getId();
    AiJob job = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, 1);
    setId(job, UUID.randomUUID());
    AiProposta proposta = new AiProposta(
        sessao, job, AiPropostaTipo.NOVA, "Consulta", "consulta", "PED-001", "Resumo",
        "<section>Consulta</section>", null, null, null, null, List.of());
    setId(proposta, UUID.randomUUID());
    when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.of(sessao));
    when(propostaRepository.findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(
        sessaoId, AiPropostaStatus.PENDENTE)).thenReturn(Optional.of(proposta));

    var response = service.rejeitar(sessaoId, " Texto genérico demais ", null);

    assertThat(response.status()).isEqualTo(AiPropostaStatus.REJEITADA);
    assertThat(response.motivoRejeicao()).isEqualTo("Texto genérico demais");
  }

  @Test
  void rejeitarSemPropostaPendenteRetorna404() throws Exception {
    AiSessao sessao = sessaoDoUsuario();
    when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));

    assertThatThrownBy(() -> service.rejeitar(sessao.getId(), null, null))
        .isInstanceOf(NotFoundException.class);
  }

  private static AiSessao sessaoDoUsuario() throws Exception {
    AiSessao sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        "Briefing completo para uma página operacional.",
        null, null, null, null, null);
    setId(sessao, UUID.randomUUID());
    sessao.setCreatedBy("system");
    return sessao;
  }

  private static void setId(Object target, UUID id) throws Exception {
    var field = target.getClass().getDeclaredField("id");
    field.setAccessible(true);
    field.set(target, id);
  }
}
