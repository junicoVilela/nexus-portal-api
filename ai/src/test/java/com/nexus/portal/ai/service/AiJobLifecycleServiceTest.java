package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobEtapa;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobLifecycleServiceTest {

  @Mock AiJobRepository jobRepository;
  @Mock AiPropostaRepository propostaRepository;
  @Mock AiEventService eventService;
  @Mock AuditoriaService auditoriaService;

  private AiJobLifecycleService service;
  private AiSessao sessao;
  private AiJob job;

  @BeforeEach
  void setUp() throws Exception {
    service = new AiJobLifecycleService(
        jobRepository, propostaRepository, eventService, auditoriaService);
    sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        "Briefing completo para uma página operacional.",
        null, null, null, null, null);
    setId(sessao, UUID.randomUUID());
    sessao.gerando();
    job = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, 2);
    setId(job, UUID.randomUUID());
    when(jobRepository.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
  }

  @Test
  void persisteEtapaEProgressoDoJob() {
    service.iniciar(job.getId(), "modelo-teste");
    service.avancar(job.getId(), AiJobEtapa.GERANDO_CONTEUDO, 50);

    assertThat(job.getStatus()).isEqualTo(AiJobStatus.PROCESSANDO);
    assertThat(job.getEtapa()).isEqualTo(AiJobEtapa.GERANDO_CONTEUDO);
    assertThat(job.getProgresso()).isEqualTo(50);
    assertThat(job.getTentativa()).isEqualTo(2);
    assertThat(job.getHeartbeatAt()).isNotNull();
  }

  @Test
  void cancelamentoImpedePersistirPropostaMesmoSeWorkerTerminar() {
    service.iniciar(job.getId(), "modelo-teste");
    sessao.cancelar();

    assertThatThrownBy(() -> service.concluir(job.getId(), resultado()))
        .isInstanceOf(AiJobCanceladoException.class);

    assertThat(job.getStatus()).isEqualTo(AiJobStatus.CANCELADO);
    assertThat(job.getEtapa()).isEqualTo(AiJobEtapa.CANCELADA);
    verify(propostaRepository, never()).save(org.mockito.ArgumentMatchers.any(AiProposta.class));
  }

  private static AiJobLifecycleService.ResultadoGeracao resultado() {
    return new AiJobLifecycleService.ResultadoGeracao(
        AiPropostaTipo.NOVA,
        "Consulta",
        "consulta",
        "CON-001",
        "Resumo",
        "<section>Consulta</section>",
        null,
        null,
        "{}",
        null,
        10,
        20,
        List.of(),
        "gerar-page-spec@2.2");
  }

  private static void setId(Object target, UUID id) throws Exception {
    var field = target.getClass().getDeclaredField("id");
    field.setAccessible(true);
    field.set(target, id);
  }
}
