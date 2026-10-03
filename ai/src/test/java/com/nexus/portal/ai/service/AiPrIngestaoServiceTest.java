package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.config.AiGithubProperties;
import com.nexus.portal.ai.config.AiGithubProperties.Repositorio;
import com.nexus.portal.ai.dto.request.AiAjustePaginaRequest;
import com.nexus.portal.ai.dto.request.CriarAiSessaoRequest;
import com.nexus.portal.ai.dto.response.AiAjustePaginaResponse;
import com.nexus.portal.ai.dto.response.AiSessaoResponse;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPrClassificacao;
import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiPrEventoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge.PaginaAjuste;
import com.nexus.portal.ai.integration.github.AiGithubClient;
import com.nexus.portal.ai.integration.github.AiGithubClient.ArquivoPr;
import com.nexus.portal.ai.integration.github.AiGithubClient.GithubIndisponivelException;
import com.nexus.portal.ai.repository.AiPrEventoRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.ai.service.AiPrIngestaoService.PullRequestMergeado;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

class AiPrIngestaoServiceTest {

  private static final UUID PROJETO = UUID.randomUUID();
  private static final UUID MODULO = UUID.randomUUID();

  private final AiPrEventoRepository eventoRepository = mock(AiPrEventoRepository.class);
  private final AiSessaoRepository sessaoRepository = mock(AiSessaoRepository.class);
  private final AiGithubClient github = mock(AiGithubClient.class);
  private final DocFlowAiBridge bridge = mock(DocFlowAiBridge.class);
  private final AiSessaoService sessaoService = mock(AiSessaoService.class);
  private final AiPropostaService propostaService = mock(AiPropostaService.class);
  private final AiAjustePaginaService ajusteService = mock(AiAjustePaginaService.class);
  private AiPrIngestaoService service;
  private AiPrEvento evento;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    var properties = new AiGithubProperties("segredo", null, null, null,
        List.of(new Repositorio("org/app", PROJETO, MODULO)), null, null, 0, 0);
    service = new AiPrIngestaoService(eventoRepository, sessaoRepository, github, properties, bridge,
        sessaoService, propostaService, ajusteService, mock(AuditoriaService.class),
        mock(PlatformTransactionManager.class), mock(ObjectProvider.class));
    evento = new AiPrEvento("d-1", "org/app", 42, "Tela de pedidos PED-001", "Adiciona filtros por status.",
        List.of(), "https://github.com/org/app/pull/42", "dev", "main", "abc", OffsetDateTime.now());
    when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));
  }

  private PullRequestMergeado pr(String delivery, String repositorio) {
    return new PullRequestMergeado(delivery, repositorio, 42, "t", null, List.of(), "u", "dev", "main", "abc", null);
  }

  private void arquivos(ArquivoPr... arquivos) {
    when(github.arquivos(eq("org/app"), eq(42), anyInt())).thenReturn(List.of(arquivos));
  }

  private static PaginaAjuste pagina(String status) {
    return new PaginaAjuste(UUID.randomUUID(), PROJETO, MODULO, "Pedidos", "pedidos", "PED-001", "r",
        "<p>x</p>", 7, status);
  }

  @Test
  void reentregaDoMesmoPrNaoDuplica() {
    when(eventoRepository.existsByDeliveryId("d-1")).thenReturn(true);
    assertThat(service.receber(pr("d-1", "org/app"))).isEmpty();

    when(eventoRepository.existsByDeliveryId("d-2")).thenReturn(false);
    when(eventoRepository.findByRepositorioAndNumeroPr("org/app", 42)).thenReturn(Optional.of(evento));
    assertThat(service.receber(pr("d-2", "org/app"))).isEmpty();
    verify(eventoRepository, never()).saveAndFlush(any());
  }

  @Test
  void repositorioForaDaConfiguracaoFicaRegistradoComoIgnorado() {
    when(eventoRepository.findByRepositorioAndNumeroPr(anyString(), anyInt())).thenReturn(Optional.empty());
    ArgumentCaptor<AiPrEvento> salvo = ArgumentCaptor.forClass(AiPrEvento.class);

    assertThat(service.receber(pr("d-9", "org/outro"))).isPresent();

    verify(eventoRepository).saveAndFlush(salvo.capture());
    assertThat(salvo.getValue().getStatus()).isEqualTo(AiPrEventoStatus.IGNORADO);
  }

  @Test
  void telaSemPaginaExistenteAbreSessaoDePaginaNovaNoModuloDoRepositorio() {
    arquivos(new ArquivoPr("src/app/pages/pedidos/pedidos.component.html", "added", 30, 0, "+<h1>Pedidos</h1>"));
    UUID sessaoId = UUID.randomUUID();
    var sessao = mock(AiSessaoResponse.class);
    when(sessao.id()).thenReturn(sessaoId);
    when(sessaoService.criar(any(), isNull())).thenReturn(sessao);
    when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.empty());

    service.processar(evento.getId());

    ArgumentCaptor<CriarAiSessaoRequest> criar = ArgumentCaptor.forClass(CriarAiSessaoRequest.class);
    verify(sessaoService).criar(criar.capture(), isNull());
    assertThat(criar.getValue().objetivo()).isEqualTo(AiObjetivo.CRIAR_PAGINA);
    assertThat(criar.getValue().moduloId()).isEqualTo(MODULO);
    assertThat(criar.getValue().briefing()).contains("org/app#42", "PED-001", "+<h1>Pedidos</h1>");
    verify(propostaService).gerar(sessaoId, null, null);
    assertThat(evento.getStatus()).isEqualTo(AiPrEventoStatus.EM_FILA);
    assertThat(evento.getClassificacao()).isEqualTo(AiPrClassificacao.UI_NOVA);
    assertThat(evento.getSessaoId()).isEqualTo(sessaoId);
  }

  @Test
  void telaDePaginaEmRascunhoPedeAjusteSobreAVersaoAtual() {
    arquivos(new ArquivoPr("src/app/pages/pedidos/pedidos.component.html", "modified", 3, 1, null));
    PaginaAjuste pagina = pagina("RASCUNHO");
    when(bridge.buscarPaginaPorCodigoTela("PED-001")).thenReturn(Optional.of(pagina));
    UUID sessaoId = UUID.randomUUID();
    when(ajusteService.pedir(eq(pagina.id()), any(), isNull())).thenReturn(new AiAjustePaginaResponse(sessaoId, null));

    service.processar(evento.getId());

    ArgumentCaptor<AiAjustePaginaRequest> pedido = ArgumentCaptor.forClass(AiAjustePaginaRequest.class);
    verify(ajusteService).pedir(eq(pagina.id()), pedido.capture(), isNull());
    assertThat(pedido.getValue().version()).isEqualTo(7L);
    assertThat(pedido.getValue().instrucao()).contains("org/app#42").hasSizeLessThanOrEqualTo(2_000);
    assertThat(evento.getStatus()).isEqualTo(AiPrEventoStatus.EM_FILA);
    assertThat(evento.getPaginaId()).isEqualTo(pagina.id());
  }

  @Test
  void paginaPublicadaAguardaVoltarARascunho() {
    arquivos(new ArquivoPr("src/app/pages/pedidos/pedidos.component.html", "modified", 3, 1, null));
    when(bridge.buscarPaginaPorCodigoTela("PED-001")).thenReturn(Optional.of(pagina("PUBLICADO")));

    service.processar(evento.getId());

    verify(ajusteService, never()).pedir(any(), any(), any());
    assertThat(evento.getStatus()).isEqualTo(AiPrEventoStatus.AGUARDANDO_RASCUNHO);
    assertThat(evento.getMensagem()).contains("publicada");
    assertThat(evento.podeReprocessar()).isTrue();
  }

  @Test
  void soBackendEhIgnorado() {
    arquivos(new ArquivoPr("api/Pedido.java", "modified", 3, 1, null));

    service.processar(evento.getId());

    assertThat(evento.getStatus()).isEqualTo(AiPrEventoStatus.IGNORADO);
    verify(sessaoService, never()).criar(any(), any());
  }

  @Test
  void falhaNoGithubViraErroReprocessavel() {
    when(github.arquivos(anyString(), anyInt(), anyInt()))
        .thenThrow(new GithubIndisponivelException("GitHub respondeu 404"));

    service.processar(evento.getId());

    assertThat(evento.getStatus()).isEqualTo(AiPrEventoStatus.ERRO);
    assertThat(evento.getMensagem()).contains("404");
    assertThat(evento.podeReprocessar()).isTrue();
  }
}
