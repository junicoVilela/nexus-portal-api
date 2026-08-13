package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.dto.request.AiConfirmarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.response.AiAplicacaoResponse;
import com.nexus.portal.ai.dto.response.AiPropostaResponse;
import com.nexus.portal.ai.entity.AiDocumentoClienteModo;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiDocumentoProjetoModo;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.entity.AiTipoDocumento;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiDocumentoImportacaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiDocumentoImportacaoServiceTest {

  private final AiDocumentoImportacaoRepository repository = mock(AiDocumentoImportacaoRepository.class);
  private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
  private final DocFlowAiBridge bridge = mock(DocFlowAiBridge.class);
  private final AiPropostaService propostaService = mock(AiPropostaService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private AiDocumentoImportacaoService service;

  @BeforeEach
  void setUp() {
    service = new AiDocumentoImportacaoService(
        repository,
        mock(AiDocumentoExtratorService.class),
        mock(AiDocumentoPlanejadorService.class),
        objectMapper,
        auditoriaService,
        bridge,
        mock(AiDocumentoAnaliseWorkerService.class),
        mock(AiSessaoService.class),
        propostaService,
        mock(com.nexus.portal.ai.repository.AiSessaoRepository.class),
        mock(com.nexus.portal.ai.config.AiProperties.class));
  }

  @Test
  void confirmarEstrutura_novoProjetoCriaContextoRealEAtualizaBriefings() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    UUID projetoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId,
        plano(moduloPlanoId, paginaPlanoId, false));
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    when(bridge.confirmarEstruturaDocumento(
        any(Boolean.class), any(), any(), any(), any(Boolean.class), any(), any(), any()))
        .thenReturn(new DocFlowAiBridge.EstruturaDocumento(
            projetoId,
            "Portal Corporativo",
            null,
            List.of(new DocFlowAiBridge.ModuloDocumentoConfirmado(
                moduloPlanoId, moduloId, "Gestão de usuários"))));
    var request = new AiConfirmarEstruturaDocumentoRequest(
        AiDocumentoProjetoModo.NOVO_PROJETO,
        AiDocumentoClienteModo.SEM_CLIENTE,
        null,
        null,
        null,
        "Portal Corporativo",
        "Manual do portal.",
        List.of(new AiConfirmarEstruturaDocumentoRequest.Modulo(
            moduloPlanoId, "Gestão de usuários")));

    var response = service.confirmarEstrutura(importacaoId, request, principal());

    assertThat(response.estruturaConfirmada()).isTrue();
    assertThat(response.projetoId()).isEqualTo(projetoId);
    assertThat(response.modulos().getFirst().moduloId()).isEqualTo(moduloId);
    assertThat(response.modulos().getFirst().paginas().getFirst().briefing())
        .contains("# Projeto: Portal Corporativo", "## Módulo: Gestão de usuários")
        .doesNotContain("Projeto sugerido", "Cadastros provisórios");
    verify(repository).flush();
    verify(auditoriaService).registrar(any(), any(), any(), any(), any());
  }

  @Test
  void confirmarEstrutura_repetidaNaoDuplicaProjetoNemModulos() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    UUID projetoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId,
        plano(moduloPlanoId, paginaPlanoId, false));
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    when(bridge.confirmarEstruturaDocumento(
        any(Boolean.class), any(), any(), any(), any(Boolean.class), any(), any(), any()))
        .thenReturn(new DocFlowAiBridge.EstruturaDocumento(
            projetoId,
            "Portal",
            null,
            List.of(new DocFlowAiBridge.ModuloDocumentoConfirmado(moduloPlanoId, moduloId, "Cadastros"))));
    var request = new AiConfirmarEstruturaDocumentoRequest(
        AiDocumentoProjetoModo.NOVO_PROJETO,
        AiDocumentoClienteModo.SEM_CLIENTE,
        null,
        null,
        null,
        "Portal",
        null,
        List.of(new AiConfirmarEstruturaDocumentoRequest.Modulo(moduloPlanoId, "Cadastros")));

    service.confirmarEstrutura(importacaoId, request, principal());
    service.confirmarEstrutura(importacaoId, request, principal());

    verify(bridge, times(1)).confirmarEstruturaDocumento(
        any(Boolean.class), any(), any(), any(), any(Boolean.class), any(), any(), any());
  }

  @Test
  void selecionarPagina_semEstruturaConfirmadaRecusaGeracao() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId,
        plano(moduloPlanoId, paginaPlanoId, false));
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));

    assertThatThrownBy(() -> service.selecionarPagina(importacaoId, paginaPlanoId, principal()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Confirme o projeto e os módulos");
  }

  @Test
  void aceitarPagina_criaRascunhoEVinculaPlanoNaMesmaOperacao() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    UUID projetoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    UUID sessaoId = UUID.randomUUID();
    UUID paginaId = UUID.randomUUID();
    var pagina = new AiDocumentoPlano.Pagina(
        paginaPlanoId,
        "Consultar usuários",
        1,
        "Briefing completo da página.",
        null,
        "FUNCIONALIDADE",
        "Funcionalidade",
        0.9,
        "Fluxo identificado.",
        AiPaginaPlanoStatus.GERADA,
        null,
        sessaoId,
        null);
    var plano = new AiDocumentoPlano(
        "Portal",
        "Manual do portal.",
        projetoId,
        null,
        true,
        List.of(new AiDocumentoPlano.Modulo(
            moduloPlanoId, moduloId, "Usuários", 1, List.of(pagina))));
    AiDocumentoImportacao importacao = importacao(importacaoId, plano);
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    AiPropostaResponse proposta = mock(AiPropostaResponse.class);
    when(proposta.paginaId()).thenReturn(null);
    when(propostaService.propostaAtual(sessaoId)).thenReturn(proposta);
    when(propostaService.aplicar(any(), any(), any())).thenReturn(new AiAplicacaoResponse(
        "PERSISTIR",
        UUID.randomUUID(),
        paginaId,
        "Consultar usuários",
        "consultar-usuarios",
        "DOC-M01-P01",
        null,
        "<p>Conteúdo</p>",
        null,
        null,
        moduloId));
    when(bridge.buscarPaginaDocumento(paginaId)).thenReturn(
        new DocFlowAiBridge.PaginaDocumento(paginaId, projetoId, moduloId, "RASCUNHO"));

    var response = service.aceitarPagina(importacaoId, paginaPlanoId, principal());

    assertThat(response.modulos().getFirst().paginas().getFirst().paginaId()).isEqualTo(paginaId);
    assertThat(response.modulos().getFirst().paginas().getFirst().status())
        .isEqualTo(AiPaginaPlanoStatus.GERADA);
    var requestCaptor = ArgumentCaptor.forClass(AplicarAiPropostaRequest.class);
    verify(propostaService).aplicar(any(), requestCaptor.capture(), any());
    assertThat(requestCaptor.getValue().ordem()).isEqualTo(1);
    verify(repository).flush();
  }

  @Test
  void aplicarSugestoesSeguras_renomeiaSemAdicionarPaginaAutomaticamente() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    UUID renomearId = UUID.randomUUID();
    UUID adicionarId = UUID.randomUUID();
    AiDocumentoPlano base = plano(moduloPlanoId, paginaPlanoId, false);
    AiDocumentoPlano plano = new AiDocumentoPlano(
        base.projetoNome(),
        base.projetoDescricao(),
        null,
        null,
        false,
        base.modulos(),
        base.projetoNomesSugeridos(),
        base.analiseOrigem(),
        base.analiseMensagem(),
        null,
        null,
        List.of(
            sugestao(
                renomearId,
                AiDocumentoSugestaoTipo.RENOMEAR_PAGINA,
                paginaPlanoId,
                null,
                moduloPlanoId,
                null,
                "Pesquisar usuários"),
            sugestao(
                adicionarId,
                AiDocumentoSugestaoTipo.ADICIONAR_PAGINA,
                null,
                null,
                null,
                moduloPlanoId,
                "Recuperar senha")));
    AiDocumentoImportacao importacao = importacao(importacaoId, plano);
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));

    var response = service.aplicarSugestoesSeguras(importacaoId, principal());

    assertThat(response.modulos().getFirst().paginas()).singleElement()
        .satisfies(pagina -> assertThat(pagina.titulo()).isEqualTo("Pesquisar usuários"));
    assertThat(response.sugestoes()).extracting(item -> item.status())
        .containsExactly(AiDocumentoSugestaoStatus.APLICADA, AiDocumentoSugestaoStatus.PENDENTE);
  }

  @Test
  void aceitarSugestao_mesclaPaginasSemPerderConteudoOriginal() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID origemId = UUID.randomUUID();
    UUID destinoId = UUID.randomUUID();
    UUID sugestaoId = UUID.randomUUID();
    AiDocumentoPlano.Pagina destino = pagina(
        destinoId, 1, "Consultar usuários", "Texto exclusivo da consulta.");
    AiDocumentoPlano.Pagina origem = pagina(
        origemId, 2, "Filtros da consulta", "Texto exclusivo dos filtros.");
    AiDocumentoPlano plano = new AiDocumentoPlano(
        "Portal",
        "Manual.",
        null,
        null,
        false,
        List.of(new AiDocumentoPlano.Modulo(
            moduloPlanoId, null, "Usuários", 1, List.of(destino, origem))),
        List.of("Portal"),
        com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem.LLM,
        "Estrutura refinada.",
        100,
        50,
        List.of(sugestao(
            sugestaoId,
            AiDocumentoSugestaoTipo.MESCLAR_PAGINAS,
            origemId,
            destinoId,
            moduloPlanoId,
            moduloPlanoId,
            "Consultar usuários e filtros")));
    AiDocumentoImportacao importacao = importacao(importacaoId, plano);
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));

    var response = service.aceitarSugestao(importacaoId, sugestaoId, principal());

    assertThat(response.modulos().getFirst().paginas()).singleElement().satisfies(resultado -> {
      assertThat(resultado.titulo()).isEqualTo("Consultar usuários e filtros");
      assertThat(resultado.briefing())
          .contains("Texto exclusivo da consulta.", "Texto exclusivo dos filtros.");
    });
    assertThat(response.sugestoes().getFirst().status())
        .isEqualTo(AiDocumentoSugestaoStatus.APLICADA);
  }

  private AiDocumentoPlano plano(UUID moduloPlanoId, UUID paginaPlanoId, boolean confirmada) {
    var pagina = new AiDocumentoPlano.Pagina(
        paginaPlanoId,
        "Consultar usuários",
        1,
        "# Projeto: Projeto sugerido\n\n## Módulo: Cadastros provisórios\n\n"
            + "### Página: Consultar usuários\n\nUse filtros para localizar os usuários.",
        null,
        "FUNCIONALIDADE",
        "Funcionalidade",
        0.8,
        "Fluxo identificado.",
        AiPaginaPlanoStatus.PENDENTE);
    return new AiDocumentoPlano(
        "Projeto sugerido",
        "Descrição sugerida.",
        null,
        null,
        confirmada,
        List.of(new AiDocumentoPlano.Modulo(
            moduloPlanoId, null, "Cadastros provisórios", 1, List.of(pagina))));
  }

  private AiDocumentoPlano.Pagina pagina(UUID id, int ordem, String titulo, String conteudo) {
    return new AiDocumentoPlano.Pagina(
        id,
        titulo,
        ordem,
        "# Projeto: Portal\n\n## Módulo: Usuários\n\n### Página: " + titulo + "\n\n" + conteudo,
        null,
        "FUNCIONALIDADE",
        "Funcionalidade",
        0.8,
        "Fluxo identificado.",
        AiPaginaPlanoStatus.PENDENTE);
  }

  private AiDocumentoPlano.Sugestao sugestao(
      UUID id,
      AiDocumentoSugestaoTipo tipo,
      UUID paginaOrigemId,
      UUID paginaDestinoId,
      UUID moduloOrigemId,
      UUID moduloDestinoId,
      String valorSugerido) {
    return new AiDocumentoPlano.Sugestao(
        id,
        tipo,
        "Melhoria sugerida",
        "A organização do manual fica mais clara.",
        0.9,
        AiDocumentoSugestaoStatus.PENDENTE,
        paginaOrigemId,
        paginaDestinoId,
        moduloOrigemId,
        moduloDestinoId,
        valorSugerido,
        "Revise o conteúdo sugerido.");
  }

  private AiDocumentoImportacao importacao(UUID id, AiDocumentoPlano plano) throws Exception {
    var importacao = new AiDocumentoImportacao(
        "manual.txt",
        AiTipoDocumento.TXT,
        "text/plain",
        100,
        "a".repeat(64),
        "Conteúdo extraído do documento.",
        1,
        objectMapper.writeValueAsString(plano),
        "[]");
    importacao.setId(id);
    importacao.setCreatedBy("editor");
    importacao.atualizarAnalise(
        objectMapper.writeValueAsString(plano),
        com.nexus.portal.ai.entity.AiImportacaoStatus.PRONTO_PARA_REVISAO);
    return importacao;
  }

  private Principal principal() {
    return () -> "editor";
  }
}
