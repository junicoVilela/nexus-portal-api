package com.nexus.portal.docflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.service.GeradorPacoteService;
import com.nexus.portal.docflow.service.PaginaAnexoService;
import com.nexus.portal.docflow.service.PaginaBlocoCatalogoService;
import com.nexus.portal.docflow.service.PaginaBlueprintCatalogoService;
import com.nexus.portal.docflow.service.PaginaEventService;
import com.nexus.portal.docflow.service.PaginaService;
import com.nexus.portal.docflow.service.PaginaTemplateService;
import com.nexus.portal.shared.exception.ConflictException;
import com.nexus.portal.shared.exception.GlobalExceptionHandler;
import java.lang.reflect.Field;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaginaControllerTest {

  @Mock PaginaService paginaService;
  @Mock PaginaAnexoService paginaAnexoService;
  @Mock PaginaBlocoCatalogoService paginaBlocoCatalogoService;
  @Mock PaginaBlueprintCatalogoService paginaBlueprintCatalogoService;
  @Mock PaginaTemplateService paginaTemplateService;
  @Mock GeradorPacoteService geradorPacoteService;
  @Mock PaginaEventService paginaEventService;
  @Mock com.nexus.portal.docflow.service.PaginaSnippetService paginaSnippetService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new PaginaController(paginaService, paginaAnexoService,
            paginaBlocoCatalogoService, paginaBlueprintCatalogoService, paginaTemplateService,
            geradorPacoteService, paginaEventService, paginaSnippetService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void listar_devolveEnvelopePaginado() throws Exception {
    when(paginaService.listar(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(pagina()), PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/api/v1/docflow/paginas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].titulo").value("Cadastro"))
        .andExpect(jsonPath("$.items[0].codigoTela").value("CAD-001"))
        .andExpect(jsonPath("$.page").value(1));
  }

  /** Campo fora da whitelist cai no sort padrão, em vez de virar ORDER BY arbitrário. */
  @Test
  void listar_comSortNaoPermitido_usaAOrdenacaoPadrao() throws Exception {
    when(paginaService.listar(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(pagina()), PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/api/v1/docflow/paginas").param("sort", "conteudoHtml"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(paginaService).listar(any(), any(), any(), any(), any(), any(), captor.capture());
    assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty))
        .doesNotContain("conteudoHtml")
        .contains("modulo.projeto.nome");
  }

  @Test
  void criar_semTitulo_devolve400() throws Exception {
    mockMvc.perform(post("/api/v1/docflow/paginas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigoTela\":\"CAD-001\",\"moduloId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(paginaService);
  }

  @Test
  void atualizar_comVersaoDivergente_devolve409() throws Exception {
    UUID id = UUID.randomUUID();
    when(paginaService.atualizar(eq(id), any(), any()))
        .thenThrow(new ConflictException("Esta página foi alterada por outro usuário."));

    mockMvc.perform(put("/api/v1/docflow/paginas/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"titulo":"Cadastro","codigoTela":"CAD-001","moduloId":"%s","version":1}
                """.formatted(UUID.randomUUID())))
        .andExpect(status().isConflict());
  }

  @Test
  void enviarRevisao_chamaOServico() throws Exception {
    UUID id = UUID.randomUUID();
    when(paginaService.enviarRevisao(eq(id), any())).thenReturn(pagina());

    mockMvc.perform(post("/api/v1/docflow/paginas/" + id + "/enviar-revisao"))
        .andExpect(status().isOk());

    verify(paginaService).enviarRevisao(eq(id), any());
  }

  @Test
  void duplicar_devolve201() throws Exception {
    UUID id = UUID.randomUUID();
    when(paginaService.duplicar(eq(id), any())).thenReturn(pagina());

    mockMvc.perform(post("/api/v1/docflow/paginas/" + id + "/duplicar"))
        .andExpect(status().isCreated());
  }

  @Test
  void reordenar_repassaAListaNaOrdemRecebida() throws Exception {
    UUID primeiro = UUID.randomUUID();
    UUID segundo = UUID.randomUUID();

    mockMvc.perform(post("/api/v1/docflow/paginas/reordenar")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"paginaIds\":[\"" + primeiro + "\",\"" + segundo + "\"]}"))
        .andExpect(status().isOk());

    verify(paginaService).reordenar(eq(List.of(primeiro, segundo)), any());
  }

  @Test
  void excluirAnexo_devolve204ERepassaOPrincipal() throws Exception {
    UUID paginaId = UUID.randomUUID();
    UUID anexoId = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/docflow/paginas/" + paginaId + "/anexos/" + anexoId))
        .andExpect(status().isNoContent());

    verify(paginaAnexoService).excluir(eq(paginaId), eq(anexoId), any());
  }

  @Test
  void templates_contamPaginasOriginadasEmUmaConsultaSo() throws Exception {
    when(paginaTemplateService.listar(any(), any(), eq(false), eq(false))).thenReturn(List.of());
    when(paginaTemplateService.paginasOriginadasPorTemplate(anyList())).thenReturn(java.util.Map.of());

    mockMvc.perform(get("/api/v1/docflow/paginas/templates"))
        .andExpect(status().isOk());

    verify(paginaTemplateService).paginasOriginadasPorTemplate(anyList());
  }

  private static Pagina pagina() throws Exception {
    Projeto projeto = new Projeto("Portal", "portal", null, true);
    setId(projeto, UUID.randomUUID());
    Modulo modulo = new Modulo("Cadastros", "cadastros", null, 1, true, projeto);
    setId(modulo, UUID.randomUUID());
    Pagina pagina = new Pagina("Cadastro", "cadastro", "CAD-001", "resumo",
        "<p>conteúdo</p>", 0, true, modulo, null);
    setId(pagina, UUID.randomUUID());
    return pagina;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
