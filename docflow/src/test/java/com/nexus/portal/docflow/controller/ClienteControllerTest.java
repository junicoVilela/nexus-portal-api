package com.nexus.portal.docflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.service.ClienteLogoService;
import com.nexus.portal.docflow.service.ClienteService;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteControllerTest {

  @Mock ClienteService clienteService;
  @Mock ClienteLogoService clienteLogoService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new ClienteController(clienteService, clienteLogoService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void listar_devolveEnvelopePaginado() throws Exception {
    when(clienteService.listar(any(), any()))
        .thenReturn(new PageImpl<>(List.of(cliente()), PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/api/v1/docflow/clientes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].nome").value("ACME"))
        .andExpect(jsonPath("$.items[0].slug").value("acme"));
  }

  @Test
  void criar_semNome_devolve400() throws Exception {
    mockMvc.perform(post("/api/v1/docflow/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"slug\":\"acme\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(clienteService);
  }

  @Test
  void buscar_inexistente_devolve404() throws Exception {
    UUID id = UUID.randomUUID();
    when(clienteService.buscar(id)).thenThrow(new NotFoundException("Cliente não encontrado."));

    mockMvc.perform(get("/api/v1/docflow/clientes/" + id))
        .andExpect(status().isNotFound());
  }

  @Test
  void excluir_comPublicacoesPendentes_devolve422() throws Exception {
    UUID id = UUID.randomUUID();
    org.mockito.Mockito.doThrow(new BusinessException("Exclua primeiro as publicações deste cliente."))
        .when(clienteService).excluir(eq(id), any());

    mockMvc.perform(delete("/api/v1/docflow/clientes/" + id))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void vincularPaginas_devolve204ERepassaAsPaginas() throws Exception {
    UUID id = UUID.randomUUID();
    UUID paginaId = UUID.randomUUID();

    mockMvc.perform(put("/api/v1/docflow/clientes/" + id + "/paginas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"paginaIds\":[\"" + paginaId + "\"]}"))
        .andExpect(status().isNoContent());

    verify(clienteService).vincularPaginas(id, List.of(paginaId));
  }

  @Test
  void vinculos_devolveAsTresColecoes() throws Exception {
    UUID id = UUID.randomUUID();
    when(clienteService.listarProjetoIds(id)).thenReturn(List.of());
    when(clienteService.listarModuloIds(id)).thenReturn(List.of());
    when(clienteService.listarPaginaIds(id)).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/docflow/clientes/" + id + "/vinculos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projetoIds").isArray())
        .andExpect(jsonPath("$.moduloIds").isArray())
        .andExpect(jsonPath("$.paginaIds").isArray());
  }

  @Test
  void copiarVinculos_semOrigem_devolve400() throws Exception {
    mockMvc.perform(post("/api/v1/docflow/clientes/" + UUID.randomUUID() + "/copiar-vinculos")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  private static Cliente cliente() throws Exception {
    Cliente cliente = new Cliente("ACME", "acme", true);
    Field f = Cliente.class.getDeclaredField("id");
    f.setAccessible(true);
    f.set(cliente, UUID.randomUUID());
    return cliente;
  }
}
