package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjetoRagServiceTest {

  private final ProjetoService projetoService = mock(ProjetoService.class);
  private final PaginaRepository paginaRepository = mock(PaginaRepository.class);
  private final PaginaSnippetService snippets = mock(PaginaSnippetService.class);
  private final ProjetoRagService service = new ProjetoRagService(
      projetoService, paginaRepository, snippets, new ManualRagService(new ObjectMapper()));
  private Projeto projeto;
  private Modulo vendas;
  private Modulo cadastros;

  @BeforeEach
  void setUp() throws Exception {
    projeto = new Projeto("Portal Vendas", "portal-vendas", null, true);
    definir(projeto, "id", UUID.randomUUID());
    vendas = new Modulo("Vendas", "vendas", null, 2, true, projeto);
    cadastros = new Modulo("Cadastros", "cadastros", null, 1, true, projeto);
    when(projetoService.buscar(projeto.getId())).thenReturn(projeto);
    when(snippets.resolver(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void exportaSoAsPaginasPublicadasDoProjetoNaOrdemDoManual() throws Exception {
    Projeto outro = new Projeto("Outro", "outro", null, true);
    definir(outro, "id", UUID.randomUUID());
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO)).thenReturn(List.of(
        pagina("Consulta de pedidos", "PED-001", vendas, "<h2>Filtros</h2><img src=\"x.png\" alt=\"Tela\">"),
        pagina("Clientes", "CLI-001", cadastros, "<p>Cadastro.</p>"),
        pagina("Fora", "OUT-001", new Modulo("M", "m", null, 0, true, outro), "<p>x</p>")));

    var exportacao = service.exportar(projeto.getId());

    assertThat(exportacao.nomeArquivo()).isEqualTo("rag-portal-vendas.zip");
    Map<String, String> arquivos = descompactar(exportacao.zip());
    assertThat(arquivos).containsKeys("llms.txt", "llms-full.txt", "rag/index.json", "rag/README.md",
        "rag/portal-vendas/PED-001.md", "rag/portal-vendas/CLI-001.md").doesNotContainKey("rag/outro/OUT-001.md");
    assertThat(arquivos.get("llms.txt").indexOf("CLI-001")).isLessThan(arquivos.get("llms.txt").indexOf("PED-001"));
    assertThat(arquivos.get("llms.txt")).contains("[Clientes](rag/portal-vendas/CLI-001.md)");
    assertThat(arquivos.get("rag/portal-vendas/PED-001.md"))
        .contains("## Filtros", "[Imagem: Tela]")
        .doesNotContain("versao:", "url:");
  }

  @Test
  void projetoSemPaginaPublicadaAvisa() {
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO)).thenReturn(List.of());
    assertThatThrownBy(() -> service.exportar(projeto.getId())).isInstanceOf(BusinessException.class);
  }

  private static Pagina pagina(String titulo, String codigo, Modulo modulo, String html) throws Exception {
    Pagina p = new Pagina(titulo, codigo.toLowerCase(), codigo, "resumo", html, 0, true, modulo, null);
    definir(p, "id", UUID.randomUUID());
    definir(p, "status", StatusPagina.PUBLICADO);
    return p;
  }

  private static Map<String, String> descompactar(byte[] zip) throws Exception {
    Map<String, String> arquivos = new LinkedHashMap<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      for (ZipEntry entrada; (entrada = in.getNextEntry()) != null; ) {
        arquivos.put(entrada.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    return arquivos;
  }

  private static void definir(Object alvo, String campo, Object valor) throws Exception {
    for (Class<?> tipo = alvo.getClass(); tipo != null; tipo = tipo.getSuperclass()) {
      try {
        Field f = tipo.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(alvo, valor);
        return;
      } catch (NoSuchFieldException ignorado) {
        // sobe para a superclasse
      }
    }
    throw new NoSuchFieldException(campo);
  }
}
