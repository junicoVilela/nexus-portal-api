package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaAnexo;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeradorManualPdfServiceTest {

  @Mock GeradorPacoteService geradorPacoteService;
  @Mock PaginaAnexoRepository paginaAnexoRepository;

  GeradorManualPdfService service;

  UUID clienteId;
  Cliente cliente;
  Projeto projeto;
  Modulo moduloA;
  Modulo moduloB;

  @Mock PaginaSnippetService paginaSnippetService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws Exception {
    service = new GeradorManualPdfService(geradorPacoteService, paginaAnexoRepository,
        paginaSnippetService);
    // Sem snippets nestes cenários: o resolvedor devolve o HTML como veio.
    org.mockito.Mockito.lenient().when(paginaSnippetService.resolver(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(inv -> inv.getArgument(0));
    clienteId = UUID.randomUUID();
    cliente = new Cliente("ACME", "acme", true);
    setId(cliente, clienteId);
    projeto = new Projeto("Suite", "suite", null, true);
    setId(projeto, UUID.randomUUID());
    moduloA = new Modulo("Cadastros", "cadastros", null, 1, true, projeto);
    setId(moduloA, UUID.randomUUID());
    moduloB = new Modulo("Operações", "operacoes", null, 2, true, projeto);
    setId(moduloB, UUID.randomUUID());
  }

  @Test
  void montarHtmlManual_falhaSemPaginas() {
    when(geradorPacoteService.selecionarPaginas(clienteId)).thenReturn(List.of());

    assertThatThrownBy(() -> service.montarHtmlManual(cliente, "1.0.0"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("páginas");
  }

  @Test
  void montarHtmlManual_montaCapaIndicePorModuloECapitulosVisiveis() throws Exception {
    Pagina clientes = pagina("Clientes", "clientes", moduloA,
        "<p>Como cadastrar clientes.</p>");
    Pagina pedidos = pagina("Pedidos", "pedidos", moduloB,
        "<p>Como acompanhar pedidos.</p>");
    when(geradorPacoteService.selecionarPaginas(clienteId)).thenReturn(List.of(clientes, pedidos));
    when(paginaAnexoRepository.findByPagina_Id(clientes.getId())).thenReturn(List.of());
    when(paginaAnexoRepository.findByPagina_Id(pedidos.getId())).thenReturn(List.of());

    String html = service.montarHtmlManual(cliente, "2.1.0");

    assertThat(html)
        .contains("Manual do usuário")
        .contains("ACME")
        .contains("2.1.0")
        .contains("Guia operacional de negócio")
        .contains("<h1>Índice</h1>")
        .contains("1. Cadastros")
        .contains("2. Operações")
        .contains("href=\"#topico-clientes\"")
        .contains("href=\"#topico-pedidos\"")
        .contains("id=\"topico-clientes\"")
        .contains("id=\"topico-pedidos\"")
        .contains("class=\"chapter\"")
        .contains("Como cadastrar clientes.")
        .contains("Como acompanhar pedidos.")
        .doesNotContain("display:none")
        .contains("max-width: 58%");
  }

  @Test
  void montarHtmlManual_embuteAnexoComoDataUriReduzido() throws Exception {
    UUID anexoId = UUID.randomUUID();
    Path imagem = tempDir.resolve("captura.png");
    Files.write(imagem, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    Pagina pagina = pagina("Login", "login", moduloA,
        "<figure class=\"screen-frame\"><img src=\"/api/v1/docflow/paginas/x/anexos/"
            + anexoId + "/download\" alt=\"Tela\"/></figure>"
            + "<div class=\"screen-placeholder\">placeholder</div>");
    PaginaAnexo anexo = new PaginaAnexo(pagina, "tela.png", "image/png", 8, imagem.toString());
    setId(anexo, anexoId);

    when(geradorPacoteService.selecionarPaginas(clienteId)).thenReturn(List.of(pagina));
    when(paginaAnexoRepository.findByPagina_Id(pagina.getId())).thenReturn(List.of(anexo));

    String html = service.montarHtmlManual(cliente, "1.0.0");

    assertThat(html)
        .contains("data:image/png;base64,")
        .contains("class=\"manual-capture\"")
        .doesNotContain("screen-placeholder")
        .doesNotContain("/anexos/" + anexoId + "/download");
  }

  private Pagina pagina(String titulo, String slug, Modulo modulo, String html) throws Exception {
    Pagina p = new Pagina(titulo, slug, slug.toUpperCase(), "resumo " + titulo, html, 1, true, modulo, null);
    setId(p, UUID.randomUUID());
    setField(p, "status", StatusPagina.PUBLICADO);
    return p;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    setField(entity, "id", id);
  }

  private static void setField(Object entity, String field, Object value) throws Exception {
    Field f = entity.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(entity, value);
  }
}
