package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  private AiPropostaService service;

  @BeforeEach
  void setUp() {
    AiProperties properties = new AiProperties(
        true, null, "sk-test", "modelo-teste", null, null, 90, 5, 1000, 20);
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
        auditoriaService);
  }

  @Test
  void cliqueRepetidoRetornaMesmoJobSemConsumirNovaGeracao() throws Exception {
    AiSessao sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        "Briefing completo para uma página operacional.",
        null, null, null, null, null);
    UUID sessaoId = UUID.randomUUID();
    setId(sessao, sessaoId);
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

  private static void setId(Object target, UUID id) throws Exception {
    var field = target.getClass().getDeclaredField("id");
    field.setAccessible(true);
    field.set(target, id);
  }
}
