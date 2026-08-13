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
import com.nexus.portal.ai.entity.AiDocumentoClienteModo;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiDocumentoProjetoModo;
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

class AiDocumentoImportacaoServiceTest {

  private final AiDocumentoImportacaoRepository repository = mock(AiDocumentoImportacaoRepository.class);
  private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
  private final DocFlowAiBridge bridge = mock(DocFlowAiBridge.class);
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
        mock(AiPropostaService.class),
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
