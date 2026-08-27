package com.nexus.portal.docflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.portal.docflow.entity.PreviewToken;
import com.nexus.portal.docflow.service.PreviewTokenService;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.GlobalExceptionHandler;
import com.nexus.portal.shared.exception.NotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreviewControllerTest {

  @Mock PreviewTokenService previewTokenService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PreviewController(previewTokenService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void gerarToken_usaValidadePadraoDe72Horas() throws Exception {
    UUID clienteId = UUID.randomUUID();
    when(previewTokenService.gerar(eq(clienteId), org.mockito.ArgumentMatchers.anyInt(), any()))
        .thenReturn(new PreviewToken(clienteId, "tok", OffsetDateTime.now().plusHours(72), "admin"));

    mockMvc.perform(post("/api/v1/preview-tokens").param("clienteId", clienteId.toString()))
        .andExpect(status().isOk());

    ArgumentCaptor<Integer> horas = ArgumentCaptor.forClass(Integer.class);
    verify(previewTokenService).gerar(eq(clienteId), horas.capture(), any());
    org.assertj.core.api.Assertions.assertThat(horas.getValue()).isEqualTo(72);
  }

  @Test
  void listar_semClienteId_devolve400() throws Exception {
    mockMvc.perform(get("/api/v1/preview-tokens"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listar_devolveOsTokensDoCliente() throws Exception {
    UUID clienteId = UUID.randomUUID();
    when(previewTokenService.listar(clienteId))
        .thenReturn(List.of(new PreviewToken(clienteId, "tok", OffsetDateTime.now().plusDays(1), "admin")));

    mockMvc.perform(get("/api/v1/preview-tokens").param("clienteId", clienteId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  void preview_devolveHtml() throws Exception {
    when(previewTokenService.renderizarPreview("tok")).thenReturn("<html><body>manual</body></html>");

    mockMvc.perform(get("/api/v1/preview/tok"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("manual")));
  }

  @Test
  void preview_comTokenExpirado_devolve422() throws Exception {
    when(previewTokenService.renderizarPreview("velho")).thenThrow(new BusinessException("Token expirado."));

    mockMvc.perform(get("/api/v1/preview/velho"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void preview_comTokenDesconhecido_devolve404() throws Exception {
    when(previewTokenService.renderizarPreview("nao-existe"))
        .thenThrow(new NotFoundException("Token inválido ou expirado."));

    mockMvc.perform(get("/api/v1/preview/nao-existe"))
        .andExpect(status().isNotFound());
  }

  @Test
  void revogar_chamaOServico() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/preview-tokens/" + id))
        .andExpect(status().isOk());

    verify(previewTokenService).revogar(eq(id), any());
  }
}
