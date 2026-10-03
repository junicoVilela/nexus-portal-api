package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.request.AiAjustePaginaRequest;
import com.nexus.portal.ai.dto.response.AiJobResponse;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAjustePaginaServiceTest {

  @Mock AiSessaoRepository sessaoRepository;
  @Mock AiMensagemRepository mensagemRepository;
  @Mock AiPropostaService propostaService;
  @Mock DocFlowAiBridge docFlowAiBridge;
  @Mock AuditoriaService auditoriaService;

  private final UUID paginaId = UUID.randomUUID();
  private AiAjustePaginaService service;

  @BeforeEach
  void setUp() {
    AiProperties properties = new AiProperties(true, null, "k", "m", null, null, null, 30, 5, 1000, 20);
    service = new AiAjustePaginaService(
        sessaoRepository, mensagemRepository, propostaService, docFlowAiBridge, properties,
        new ObjectMapper(), auditoriaService);
  }

  private void pagina(String status, long version, String html) {
    when(docFlowAiBridge.buscarPaginaParaAjuste(paginaId)).thenReturn(new DocFlowAiBridge.PaginaAjuste(
        paginaId, UUID.randomUUID(), UUID.randomUUID(), "Consulta", "consulta", "PED-001", null, html,
        version, status));
  }

  @Test
  void criaSessaoNaVersaoAtualEEnfileiraAGeracao() {
    pagina("RASCUNHO", 3, "<h2>Pré-requisitos</h2><p>Perfil operador.</p>");
    when(sessaoRepository.save(any(AiSessao.class))).thenAnswer(inv -> {
      AiSessao sessao = inv.getArgument(0);
      var campo = AiSessao.class.getDeclaredField("id");
      campo.setAccessible(true);
      campo.set(sessao, UUID.randomUUID());
      return sessao;
    });
    when(propostaService.gerar(any(), any())).thenReturn(new AiJobResponse(
        UUID.randomUUID(), null, null, null, null, 0, 1, null, null, null, null, null, 0L, null, null, null, null));

    var resposta = service.pedir(paginaId, new AiAjustePaginaRequest("Inclua o perfil gestor.", "s1", 3L), null);

    ArgumentCaptor<AiSessao> sessao = ArgumentCaptor.forClass(AiSessao.class);
    verify(sessaoRepository).save(sessao.capture());
    assertThat(sessao.getValue().getObjetivo()).isEqualTo(AiObjetivo.ATUALIZAR_PAGINA);
    assertThat(sessao.getValue().getVersionBase()).isEqualTo(3L);
    assertThat(sessao.getValue().getSecaoId()).isEqualTo("s1");
    assertThat(sessao.getValue().getStatus()).isEqualTo(AiSessaoStatus.PRONTA_PARA_GERAR);
    ArgumentCaptor<AiMensagem> mensagem = ArgumentCaptor.forClass(AiMensagem.class);
    verify(mensagemRepository).save(mensagem.capture());
    assertThat(AiPayloadJson.lerInstrucao(new ObjectMapper(), mensagem.getValue().getPayloadJson()))
        .isEqualTo("Inclua o perfil gestor.");
    verify(propostaService).gerar(eq(resposta.sessaoId()), any());
  }

  @Test
  void paginaPublicadaPrecisaVoltarParaRascunho() {
    pagina("PUBLICADO", 3, "<p>x</p>");
    assertThatThrownBy(() -> service.pedir(paginaId, new AiAjustePaginaRequest("Inclua o perfil gestor.", null, 3L), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("volte para rascunho");
    verify(sessaoRepository, never()).save(any());
  }

  @Test
  void versaoDiferenteDoEditorRetorna409() {
    pagina("RASCUNHO", 4, "<p>x</p>");
    assertThatThrownBy(() -> service.pedir(paginaId, new AiAjustePaginaRequest("Inclua o perfil gestor.", null, 3L), null))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void secaoInexistenteEhRecusada() {
    pagina("EM_REVISAO", 3, "<h2>Único</h2><p>x</p>");
    assertThatThrownBy(() -> service.pedir(paginaId, new AiAjustePaginaRequest("Inclua o perfil gestor.", "s9", 3L), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("s9");
  }

  @Test
  void paginaGrandeSemSecaoPedeParaEscolherUma() {
    pagina("RASCUNHO", 3, "<p>linha</p>".repeat(AiPagePatchService.MAX_UNIDADES_ESBOCO + 1));
    assertThatThrownBy(() -> service.pedir(paginaId, new AiAjustePaginaRequest("Inclua o perfil gestor.", null, 3L), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Escolha uma seção");
  }
}
