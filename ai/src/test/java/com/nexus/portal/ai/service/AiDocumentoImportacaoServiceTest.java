package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.dto.request.AiAtualizarComposicaoDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiConfirmarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiReordenarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.response.AiAplicacaoResponse;
import com.nexus.portal.ai.dto.response.AiComponenteCandidatoResponse;
import com.nexus.portal.ai.dto.response.AiPropostaResponse;
import com.nexus.portal.ai.dto.response.AiTemplateRecomendacaoResponse;
import com.nexus.portal.ai.entity.AiImportacaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoClienteModo;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiDocumentoProjetoModo;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
import com.nexus.portal.ai.entity.AiPaginaPlanoOrigem;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.entity.AiTipoDocumento;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiDocumentoImportacaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.ConflictException;
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
  private final AiTemplateRecomendacaoService templateService = mock(AiTemplateRecomendacaoService.class);
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
        mock(com.nexus.portal.ai.config.AiProperties.class),
        templateService);
  }

  @Test
  void importar_mesmoArquivoEmAndamentoRetomaEmVezDeDuplicar() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    AiDocumentoImportacao existente = importacao(importacaoId, plano(UUID.randomUUID(), UUID.randomUUID(), true));
    when(repository.findFirstByCreatedByAndHashSha256AndStatusNotOrderByUpdatedAtDesc(
        eq("editor"), any(), eq(AiImportacaoStatus.CONCLUIDA))).thenReturn(Optional.of(existente));
    var arquivo = new org.springframework.mock.web.MockMultipartFile(
        "arquivo", "manual.txt", "text/plain", "# Manual".getBytes());

    var response = service.importar(arquivo, null, null, false, principal());

    assertThat(response.retomada()).isTrue();
    assertThat(response.id()).isEqualTo(importacaoId);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void importar_novaImportacaoIgnoraAExistente() {
    var arquivo = new org.springframework.mock.web.MockMultipartFile(
        "arquivo", "manual.txt", "text/plain", "# Manual".getBytes());
    try {
      service.importar(arquivo, null, null, true, principal());
    } catch (RuntimeException ignorada) {
      // extrator mockado devolve nulo; só interessa que a busca por duplicado não aconteceu
    }
    verify(repository, never()).findFirstByCreatedByAndHashSha256AndStatusNotOrderByUpdatedAtDesc(any(), any(), any());
  }

  @Test
  void emAndamento_resumeProgressoDasPaginas() throws Exception {
    AiDocumentoImportacao importacao = importacao(UUID.randomUUID(), plano(UUID.randomUUID(), UUID.randomUUID(), true));
    when(repository.findTop10ByCreatedByAndStatusNotOrderByUpdatedAtDesc("editor", AiImportacaoStatus.CONCLUIDA))
        .thenReturn(List.of(importacao));

    var lista = service.emAndamento(principal());

    assertThat(lista).hasSize(1);
    assertThat(lista.getFirst().paginasTotal()).isEqualTo(1);
    assertThat(lista.getFirst().paginasRevisadas()).isZero();
    assertThat(lista.getFirst().estruturaConfirmada()).isTrue();
  }

  @Test
  void atualizarComposicao_persisteOrdemAprovadaEBloqueiaEssenciais() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId,
        plano(moduloPlanoId, paginaPlanoId, false));
    List<String> selecionados = List.of("introducao", "passo-a-passo", "resultado-esperado");
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    when(templateService.validarComponentes(any(), any(), any(), any(), any()))
        .thenReturn(selecionados);
    var essencial = new AiComponenteCandidatoResponse(
        "introducao", "Introdução", "Contexto", "Estrutura", "intro",
        "OBRIGATORIA", true, "Essencial.");
    var recomendacao = new AiTemplateRecomendacaoResponse(
        null, List.of(), false, "fluxo-guiado", "Fluxo guiado", 45, List.of(essencial));
    when(templateService.recomendar(any())).thenReturn(recomendacao);

    var response = service.atualizarComposicao(
        importacaoId,
        paginaPlanoId,
        new AiAtualizarComposicaoDocumentoRequest(importacao.getVersion(), selecionados),
        principal());

    var pagina = response.modulos().getFirst().paginas().getFirst();
    assertThat(pagina.componentesSelecionados()).containsExactlyElementsOf(selecionados);
    assertThat(pagina.componentesObrigatorios()).containsExactly("introducao");
    assertThat(pagina.blueprintNome()).isEqualTo("Fluxo guiado");
    assertThat(pagina.composicaoAjustadaManualmente()).isTrue();
    verify(repository).flush();
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
  void confirmarEstrutura_comModuloVazioSolicitaOrganizacao() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloPlanoId = UUID.randomUUID();
    UUID moduloVazioId = UUID.randomUUID();
    UUID paginaPlanoId = UUID.randomUUID();
    AiDocumentoPlano base = plano(moduloPlanoId, paginaPlanoId, false);
    AiDocumentoPlano comModuloVazio = new AiDocumentoPlano(
        base.projetoNome(),
        base.projetoDescricao(),
        base.projetoId(),
        base.clienteId(),
        false,
        List.of(
            base.modulos().getFirst(),
            new AiDocumentoPlano.Modulo(
                moduloVazioId, null, "Relatórios", 2, List.of())),
        base.projetoNomesSugeridos(),
        base.analiseOrigem(),
        base.analiseMensagem(),
        base.tokensEntradaAnalise(),
        base.tokensSaidaAnalise(),
        base.sugestoes());
    AiDocumentoImportacao importacao = importacao(importacaoId, comModuloVazio);
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    var request = new AiConfirmarEstruturaDocumentoRequest(
        AiDocumentoProjetoModo.NOVO_PROJETO,
        AiDocumentoClienteModo.SEM_CLIENTE,
        null,
        null,
        null,
        "Portal",
        null,
        List.of(
            new AiConfirmarEstruturaDocumentoRequest.Modulo(
                moduloPlanoId, "Cadastros provisórios"),
            new AiConfirmarEstruturaDocumentoRequest.Modulo(
                moduloVazioId, "Relatórios")));

    assertThatThrownBy(() -> service.confirmarEstrutura(importacaoId, request, principal()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("módulos vazios");
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
    when(propostaService.propostaAtual(org.mockito.ArgumentMatchers.eq(sessaoId), any())).thenReturn(proposta);
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

  @Test
  void reordenarEstrutura_movePaginaEntreModulosEPreservaConteudo() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloUsuariosId = UUID.randomUUID();
    UUID moduloSegurancaId = UUID.randomUUID();
    UUID consultaId = UUID.randomUUID();
    UUID permissoesId = UUID.randomUUID();
    UUID senhaId = UUID.randomUUID();
    UUID sugestaoId = UUID.randomUUID();
    AiDocumentoPlano plano = new AiDocumentoPlano(
        "Portal",
        "Manual.",
        null,
        null,
        false,
        List.of(
            new AiDocumentoPlano.Modulo(
                moduloUsuariosId,
                null,
                "Usuários",
                1,
                List.of(
                    pagina(consultaId, 1, "Consultar usuários", "Texto exclusivo da consulta."),
                    pagina(permissoesId, 2, "Permissões", "Texto exclusivo das permissões."))),
            new AiDocumentoPlano.Modulo(
                moduloSegurancaId,
                null,
                "Segurança",
                2,
                List.of(pagina(senhaId, 1, "Alterar senha", "Texto exclusivo da senha.")))),
        List.of("Portal"),
        com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem.LLM,
        "Estrutura refinada.",
        100,
        50,
        List.of(sugestao(
            sugestaoId,
            AiDocumentoSugestaoTipo.MOVER_PAGINA,
            permissoesId,
            null,
            moduloUsuariosId,
            moduloSegurancaId,
            null)));
    AiDocumentoImportacao importacao = importacao(importacaoId, plano);
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    var request = new AiReordenarEstruturaDocumentoRequest(
        0L,
        List.of(
            new AiReordenarEstruturaDocumentoRequest.Modulo(
                moduloSegurancaId,
                "Segurança",
                List.of(
                    paginaRequest(senhaId, "Alterar senha", "Texto exclusivo da senha."),
                    paginaRequest(permissoesId, "Permissões", "Texto exclusivo das permissões."))),
            new AiReordenarEstruturaDocumentoRequest.Modulo(
                moduloUsuariosId,
                "Usuários",
                List.of(paginaRequest(
                    consultaId, "Consultar usuários", "Texto exclusivo da consulta.")))));

    var response = service.reordenarEstrutura(importacaoId, request, principal());

    assertThat(response.modulos()).extracting(item -> item.nome())
        .containsExactly("Segurança", "Usuários");
    assertThat(response.modulos().getFirst().paginas()).extracting(item -> item.titulo())
        .containsExactly("Alterar senha", "Permissões");
    assertThat(response.modulos().getFirst().paginas().get(1).briefing())
        .contains("## Módulo: Segurança", "Texto exclusivo das permissões.");
    assertThat(response.sugestoes().getFirst().status())
        .isEqualTo(AiDocumentoSugestaoStatus.APLICADA);
    var restaurada = service.reordenarEstrutura(
        importacaoId,
        new AiReordenarEstruturaDocumentoRequest(
            0L,
            List.of(
                new AiReordenarEstruturaDocumentoRequest.Modulo(
                    moduloUsuariosId,
                    "Usuários",
                    List.of(
                        paginaRequest(consultaId, "Consultar usuários", "Texto exclusivo da consulta."),
                        paginaRequest(permissoesId, "Permissões", "Texto exclusivo das permissões."))),
                new AiReordenarEstruturaDocumentoRequest.Modulo(
                    moduloSegurancaId,
                    "Segurança",
                    List.of(paginaRequest(
                        senhaId, "Alterar senha", "Texto exclusivo da senha."))))),
        principal());
    assertThat(restaurada.sugestoes().getFirst().status())
        .isEqualTo(AiDocumentoSugestaoStatus.PENDENTE);
    verify(repository, times(2)).flush();
  }

  @Test
  void reordenarEstrutura_comVersaoDesatualizadaRecusaSobrescrita() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    UUID paginaId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId, plano(moduloId, paginaId, false));
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    var request = new AiReordenarEstruturaDocumentoRequest(
        1L,
        List.of(new AiReordenarEstruturaDocumentoRequest.Modulo(
            moduloId,
            "Cadastros",
            List.of(paginaRequest(
                paginaId, "Consultar usuários", "Use filtros para localizar os usuários.")))));

    assertThatThrownBy(() -> service.reordenarEstrutura(importacaoId, request, principal()))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("alterada em outra tela");
  }

  @Test
  void reordenarEstrutura_permiteCriarERemoverModuloVazio() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    UUID novoModuloId = UUID.randomUUID();
    UUID paginaId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId, plano(moduloId, paginaId, false));
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));
    var request = new AiReordenarEstruturaDocumentoRequest(
        0L,
        List.of(
            new AiReordenarEstruturaDocumentoRequest.Modulo(
                moduloId,
                "Cadastros provisórios",
                List.of(paginaRequest(
                    paginaId, "Consultar usuários", "Use filtros para localizar os usuários."))),
            new AiReordenarEstruturaDocumentoRequest.Modulo(
                novoModuloId, "Relatórios", List.of())));

    var criada = service.reordenarEstrutura(importacaoId, request, principal());
    assertThat(criada.modulos()).extracting(item -> item.nome())
        .containsExactly("Cadastros provisórios", "Relatórios");
    assertThat(criada.modulos().get(1).paginas()).isEmpty();

    var removida = service.reordenarEstrutura(
        importacaoId,
        new AiReordenarEstruturaDocumentoRequest(
            0L,
            List.of(new AiReordenarEstruturaDocumentoRequest.Modulo(
                moduloId,
                "Cadastros provisórios",
                List.of(paginaRequest(
                    paginaId, "Consultar usuários", "Use filtros para localizar os usuários."))))),
        principal());

    assertThat(removida.modulos()).extracting(item -> item.nome())
        .containsExactly("Cadastros provisórios");
    verify(repository, times(2)).flush();
  }

  @Test
  void reordenarEstrutura_editaCriaERemovePaginasMantendoOrigem() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    UUID paginaOriginalId = UUID.randomUUID();
    UUID paginaManualId = UUID.randomUUID();
    AiDocumentoImportacao importacao = importacao(
        importacaoId, plano(moduloId, paginaOriginalId, false));
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));

    var atualizada = service.reordenarEstrutura(
        importacaoId,
        new AiReordenarEstruturaDocumentoRequest(
            0L,
            List.of(new AiReordenarEstruturaDocumentoRequest.Modulo(
                moduloId,
                "Cadastros provisórios",
                List.of(
                    new AiReordenarEstruturaDocumentoRequest.Pagina(
                        paginaOriginalId,
                        "Pesquisar usuários",
                        "Use filtros avançados para localizar os usuários.",
                        AiPaginaPlanoOrigem.DOCUMENTO,
                        true),
                    new AiReordenarEstruturaDocumentoRequest.Pagina(
                        paginaManualId,
                        "Exportar usuários",
                        "Clique em Exportar para baixar o resultado da consulta.",
                        AiPaginaPlanoOrigem.MANUAL,
                        true))))),
        principal());

    assertThat(atualizada.modulos().getFirst().paginas())
        .extracting(item -> item.titulo())
        .containsExactly("Pesquisar usuários", "Exportar usuários");
    assertThat(atualizada.modulos().getFirst().paginas().getFirst().briefing())
        .contains("### Página: Pesquisar usuários", "filtros avançados");
    assertThat(atualizada.modulos().getFirst().paginas().getFirst().origem())
        .isEqualTo(AiPaginaPlanoOrigem.DOCUMENTO);
    assertThat(atualizada.modulos().getFirst().paginas().getFirst().ajustadaManualmente()).isTrue();
    assertThat(atualizada.modulos().getFirst().paginas().get(1).origem())
        .isEqualTo(AiPaginaPlanoOrigem.MANUAL);
    assertThat(atualizada.modulos().getFirst().paginas().get(1).templateId()).isNull();

    var removida = service.reordenarEstrutura(
        importacaoId,
        new AiReordenarEstruturaDocumentoRequest(
            0L,
            List.of(new AiReordenarEstruturaDocumentoRequest.Modulo(
                moduloId,
                "Cadastros provisórios",
                List.of(new AiReordenarEstruturaDocumentoRequest.Pagina(
                    paginaOriginalId,
                    "Pesquisar usuários",
                    "Use filtros avançados para localizar os usuários.",
                    AiPaginaPlanoOrigem.DOCUMENTO,
                    true))))),
        principal());

    assertThat(removida.modulos().getFirst().paginas()).singleElement()
        .satisfies(item -> assertThat(item.titulo()).isEqualTo("Pesquisar usuários"));
  }

  @Test
  void reordenarEstrutura_naoAlteraPaginaJaGerada() throws Exception {
    UUID importacaoId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    UUID paginaId = UUID.randomUUID();
    AiDocumentoPlano base = plano(moduloId, paginaId, false);
    AiDocumentoPlano.Pagina original = base.modulos().getFirst().paginas().getFirst();
    AiDocumentoPlano.Pagina gerada = new AiDocumentoPlano.Pagina(
        original.id(),
        original.titulo(),
        original.ordem(),
        original.briefing(),
        original.templateId(),
        original.templateCodigo(),
        original.templateNome(),
        original.confiancaTemplate(),
        original.motivoTemplate(),
        AiPaginaPlanoStatus.GERADA,
        UUID.randomUUID(),
        null,
        null,
        AiPaginaPlanoOrigem.DOCUMENTO,
        false);
    AiDocumentoPlano plano = new AiDocumentoPlano(
        base.projetoNome(),
        base.projetoDescricao(),
        null,
        null,
        false,
        List.of(new AiDocumentoPlano.Modulo(moduloId, null, "Cadastros provisórios", 1, List.of(gerada))));
    AiDocumentoImportacao importacao = importacao(importacaoId, plano);
    when(repository.findByIdAndCreatedBy(importacaoId, "editor")).thenReturn(Optional.of(importacao));

    var request = new AiReordenarEstruturaDocumentoRequest(
        0L,
        List.of(new AiReordenarEstruturaDocumentoRequest.Modulo(
            moduloId,
            "Cadastros provisórios",
            List.of(new AiReordenarEstruturaDocumentoRequest.Pagina(
                paginaId,
                "Título alterado",
                "Conteúdo que não deve substituir a página gerada.",
                AiPaginaPlanoOrigem.DOCUMENTO,
                true)))));

    assertThatThrownBy(() -> service.reordenarEstrutura(importacaoId, request, principal()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já foi gerada");
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

  private AiReordenarEstruturaDocumentoRequest.Pagina paginaRequest(
      UUID id, String titulo, String conteudo) {
    return new AiReordenarEstruturaDocumentoRequest.Pagina(
        id, titulo, conteudo, AiPaginaPlanoOrigem.DOCUMENTO, false);
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
