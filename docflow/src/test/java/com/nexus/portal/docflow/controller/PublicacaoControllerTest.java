package com.nexus.portal.docflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.service.GeradorPdfService;
import com.nexus.portal.docflow.service.PublicacaoEventService;
import com.nexus.portal.docflow.service.PublicacaoService;
import com.nexus.portal.shared.config.JwtService;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.GlobalExceptionHandler;
import com.nexus.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Fatia HTTP do controller: mapeamento, validação do corpo, envelope de
 * paginação e tradução de exceção em status. As permissões declaradas por
 * endpoint são cobertas em {@link PermissoesDosEndpointsTest}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacaoControllerTest {

  @Mock PublicacaoService publicacaoService;
  @Mock PublicacaoEventService publicacaoEventService;
  @Mock com.nexus.portal.docflow.service.PublicacaoDiffService publicacaoDiffService;
  @Mock com.nexus.portal.docflow.service.PublicacaoConteudoService publicacaoConteudoService;
  @Mock GeradorPdfService geradorPdfService;
  @Mock JwtService jwtService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PublicacaoController(publicacaoService, publicacaoEventService,
            publicacaoDiffService, publicacaoConteudoService, geradorPdfService, jwtService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void listar_devolveEnvelopePaginado() throws Exception {
    when(publicacaoService.listar(any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(publicacao(StatusPublicacao.SUCESSO)),
            PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/api/v1/docflow/publicacoes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].versao").value("1.0.0"))
        .andExpect(jsonPath("$.items[0].status").value("SUCESSO"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.totalItems").value(1));
  }

  @Test
  void listar_comFiltroDeStatusInvalido_naoDerrubaOEndpoint() throws Exception {
    mockMvc.perform(get("/api/v1/docflow/publicacoes").param("status", "INEXISTENTE"))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void gerar_comCorpoValido_devolve201() throws Exception {
    UUID clienteId = UUID.randomUUID();
    when(publicacaoService.gerar(eq(clienteId), eq("1.0.0"), any(), any()))
        .thenReturn(publicacao(StatusPublicacao.GERANDO));

    mockMvc.perform(post("/api/v1/docflow/publicacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clienteId\":\"" + clienteId + "\",\"versao\":\"1.0.0\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("GERANDO"))
        .andExpect(jsonPath("$.cancelamentoSolicitado").value(false));
  }

  @Test
  void gerar_semVersao_devolve400EnaoChamaOServico() throws Exception {
    mockMvc.perform(post("/api/v1/docflow/publicacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clienteId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(publicacaoService);
  }

  @Test
  void cancelar_chamaOServico() throws Exception {
    UUID id = UUID.randomUUID();
    when(publicacaoService.cancelar(eq(id), any())).thenReturn(publicacao(StatusPublicacao.GERANDO));

    mockMvc.perform(post("/api/v1/docflow/publicacoes/" + id + "/cancelar"))
        .andExpect(status().isOk());

    verify(publicacaoService).cancelar(eq(id), any());
  }

  @Test
  void buscar_inexistente_devolve404() throws Exception {
    UUID id = UUID.randomUUID();
    when(publicacaoService.buscar(id)).thenThrow(new NotFoundException("Publicação não encontrada."));

    mockMvc.perform(get("/api/v1/docflow/publicacoes/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Publicação não encontrada."));
  }

  /** Regra de negócio violada vira 422 (o 400 fica para corpo malformado). */
  @Test
  void download_dePublicacaoNaoConcluida_devolve422() throws Exception {
    UUID id = UUID.randomUUID();
    when(publicacaoService.buscar(id)).thenReturn(publicacao(StatusPublicacao.GERANDO));
    when(publicacaoService.recursoPacoteDownloadPublico(id))
        .thenThrow(new BusinessException("Download disponível apenas para publicações concluídas."));

    mockMvc.perform(get("/api/v1/docflow/publicacoes/" + id + "/download"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void excluir_devolve204() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/docflow/publicacoes/" + id))
        .andExpect(status().isNoContent());

    verify(publicacaoService).excluir(eq(id), any());
  }

  @Test
  void reprocessarLote_semIds_devolve400() throws Exception {
    mockMvc.perform(post("/api/v1/docflow/publicacoes/reprocessar-lote")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ids\":[]}"))
        .andExpect(status().isBadRequest());
  }

  private static Publicacao publicacao(StatusPublicacao status) throws Exception {
    Cliente cliente = new Cliente("ACME", "acme", true);
    setId(cliente, UUID.randomUUID());
    Publicacao publicacao = new Publicacao(cliente, "1.0.0", null);
    setId(publicacao, UUID.randomUUID());
    if (status == StatusPublicacao.SUCESSO) {
      publicacao.registrarSucesso(3, 1, "manual.zip", "/tmp/manual.zip", "sha", "{}");
    }
    return publicacao;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
