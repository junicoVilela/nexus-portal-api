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
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest.ModoAplicacao;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.ConflictException;
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
        mensagemRepository,
        new AiPagePatchService(new ObjectMapper(), docFlowAiBridge, new AiHtmlSanitizer()));
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
        "<section>Consulta</section>", null, null, null, null, List.of(), null, null);
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

  @Test
  void aplicarAjusteReaplicaSoAsOperacoesAceitasSobreAVersaoBase() throws Exception {
    var cenario = cenarioAjuste(7L);

    var aplicacao = service.aplicar(
        cenario.sessao().getId(),
        new AplicarAiPropostaRequest(ModoAplicacao.FORM, null, null, null, List.of("op2")),
        null);

    assertThat(aplicacao.conteudoHtml()).contains("Passo revisado.").contains("Pré-requisito original.");
    assertThat(cenario.proposta().getStatus()).isEqualTo(AiPropostaStatus.ACEITA);
    assertThat(cenario.proposta().getOperacoesAceitas()).containsExactly("op2");
    assertThat(cenario.sessao().getStatus()).isEqualTo(AiSessaoStatus.APLICADA);
  }

  @Test
  void aplicarAjusteSobrePaginaQueMudouRetorna409() throws Exception {
    var cenario = cenarioAjuste(8L);

    assertThatThrownBy(() -> service.aplicar(
        cenario.sessao().getId(), new AplicarAiPropostaRequest(ModoAplicacao.FORM, null, null, null), null))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("mudou desde a proposta");
    assertThat(cenario.proposta().getStatus()).isEqualTo(AiPropostaStatus.PENDENTE);
  }

  @Test
  void aplicarAjusteRecusaOperacaoDeOutraProposta() throws Exception {
    var cenario = cenarioAjuste(7L);

    assertThatThrownBy(() -> service.aplicar(
        cenario.sessao().getId(),
        new AplicarAiPropostaRequest(ModoAplicacao.FORM, null, null, null, List.of("op9")),
        null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void vincularPaginaAceitaAPropostaAplicadaNoEditor() throws Exception {
    AiSessao sessao = sessaoDoUsuario();
    AiJob job = new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, 1);
    AiProposta proposta = new AiProposta(sessao, job, AiPropostaTipo.NOVA, "Consulta", "consulta", "PED-001",
        null, "<p>x</p>", null, null, null, null, List.of(), "gerar-page-spec@2.2", null);
    setId(proposta, UUID.randomUUID());
    UUID paginaId = UUID.randomUUID();
    when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
    when(propostaRepository.findFirstBySessaoIdOrderByCreatedAtDesc(sessao.getId())).thenReturn(Optional.of(proposta));
    when(propostaRepository.findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(sessao.getId(), AiPropostaStatus.PENDENTE))
        .thenReturn(Optional.of(proposta));

    var resposta = service.vincularPagina(sessao.getId(), paginaId, null);

    assertThat(resposta.status()).isEqualTo(AiPropostaStatus.ACEITA);
    assertThat(proposta.getPaginaId()).isEqualTo(paginaId);
    assertThat(sessao.getStatus()).isEqualTo(AiSessaoStatus.APLICADA);

    // Segundo aviso do editor (ex.: retry) não falha nem duplica.
    assertThat(service.vincularPagina(sessao.getId(), paginaId, null).status()).isEqualTo(AiPropostaStatus.ACEITA);
  }

  private record CenarioAjuste(AiSessao sessao, AiProposta proposta) {}

  /** Página na versão {@code versaoAtual}; a sessão foi aberta na versão 7. */
  private CenarioAjuste cenarioAjuste(long versaoAtual) throws Exception {
    String html = "<h2>Pré-requisitos</h2><p>Pré-requisito original.</p><h2>Passo</h2><p>Passo original.</p>";
    UUID paginaId = UUID.randomUUID();
    AiSessao sessao = AiSessao.ajuste("Revise o passo.", null, null, paginaId, 7L, null);
    setId(sessao, UUID.randomUUID());
    sessao.setCreatedBy("system");
    sessao.pronta();
    AiJob job = new AiJob(sessao, AiJobTipo.AJUSTAR, 1);
    setId(job, UUID.randomUUID());
    AiPagePatch patch = new AiPagePatch("ajuste", List.of(
        new AiPagePatch.Operacao("op1", AiPagePatch.Tipo.ALTERAR_TEXTO, "u2", "Pré-requisito original.",
            "Pré-requisito revisado.", null, null, null, "a"),
        new AiPagePatch.Operacao("op2", AiPagePatch.Tipo.ALTERAR_TEXTO, "u4", "Passo original.",
            "Passo revisado.", null, null, null, "b")));
    AiProposta proposta = new AiProposta(
        sessao, job, AiPropostaTipo.ATUALIZACAO, "Página", "pagina", "PED-001", null, html,
        null, null, null, null, List.of(), "ajustar-pagina@1.1",
        new ObjectMapper().writeValueAsString(patch));
    setId(proposta, UUID.randomUUID());
    when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
    when(propostaRepository.findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(
        sessao.getId(), AiPropostaStatus.PENDENTE)).thenReturn(Optional.of(proposta));
    when(docFlowAiBridge.buscarPaginaParaAjuste(paginaId)).thenReturn(new DocFlowAiBridge.PaginaAjuste(
        paginaId, null, null, "Página", "pagina", "PED-001", null, html, versaoAtual, "RASCUNHO"));
    return new CenarioAjuste(sessao, proposta);
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
