package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.repository.ClienteModuloRepository;
import com.nexus.portal.docflow.repository.ClientePaginaRepository;
import com.nexus.portal.docflow.repository.ClienteProjetoRepository;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.service.GeradorPacoteService.ResultadoGeracao;
import com.nexus.portal.shared.config.StorageProperties;
import com.nexus.portal.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeradorPacoteServiceTest {

  @Mock ClienteModuloRepository clienteModuloRepository;
  @Mock ClientePaginaRepository clientePaginaRepository;
  @Mock ClienteProjetoRepository clienteProjetoRepository;
  @Mock PaginaRepository paginaRepository;
  @Mock PaginaAnexoRepository paginaAnexoRepository;
  @Mock EmpresaLogoService empresaLogoService;

  GeradorPacoteService service;

  UUID clienteId;
  Cliente cliente;
  Projeto projeto;
  Modulo modulo;

  @TempDir Path storageDir;

  @BeforeEach
  void setUp() throws Exception {
    StorageProperties props = new StorageProperties(storageDir.toString());
    service = new GeradorPacoteService(clienteModuloRepository, clientePaginaRepository,
        paginaRepository, clienteProjetoRepository, paginaAnexoRepository, props,
        empresaLogoService, new ObjectMapper());

    clienteId = UUID.randomUUID();
    cliente = new Cliente("ACME", "acme", true);
    setId(cliente, clienteId);

    projeto = new Projeto("Suite", "suite", null, true);
    setId(projeto, UUID.randomUUID());

    modulo = new Modulo("Portal", "portal", null, 1, true, projeto);
    setId(modulo, UUID.randomUUID());

    when(clienteModuloRepository.findModuloIdsByClienteId(clienteId)).thenReturn(List.of());
    when(clientePaginaRepository.findPaginaIdsByClienteId(clienteId)).thenReturn(List.of());
    when(clienteProjetoRepository.findProjetoIdsByClienteId(clienteId)).thenReturn(List.of());
    when(empresaLogoService.encontrar()).thenReturn(Optional.empty());
    when(paginaAnexoRepository.findByPagina_Id(org.mockito.ArgumentMatchers.any(UUID.class)))
        .thenReturn(List.of());
  }

  @Test
  void selecionarPaginas_incluiViaModuloDoCliente() throws Exception {
    Pagina p = paginaPublicada("Login", "login", 1);
    when(clienteModuloRepository.findModuloIdsByClienteId(clienteId))
        .thenReturn(List.of(modulo.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(p));

    List<Pagina> resultado = service.selecionarPaginas(clienteId);

    assertThat(resultado).containsExactly(p);
  }

  @Test
  void selecionarPaginas_incluiViaProjetoDoCliente() throws Exception {
    Pagina p = paginaPublicada("Login", "login", 1);
    when(clienteProjetoRepository.findProjetoIdsByClienteId(clienteId))
        .thenReturn(List.of(projeto.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(p));

    List<Pagina> resultado = service.selecionarPaginas(clienteId);

    assertThat(resultado).containsExactly(p);
  }

  @Test
  void selecionarPaginas_incluiViaVinculoDireto() throws Exception {
    Pagina p = paginaPublicada("Login", "login", 1);
    when(clientePaginaRepository.findPaginaIdsByClienteId(clienteId))
        .thenReturn(List.of(p.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(p));

    List<Pagina> resultado = service.selecionarPaginas(clienteId);

    assertThat(resultado).containsExactly(p);
  }

  @Test
  void selecionarPaginas_excluiPaginaSemVinculoAlgum() throws Exception {
    Pagina p = paginaPublicada("Login", "login", 1);
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(p));

    List<Pagina> resultado = service.selecionarPaginas(clienteId);

    assertThat(resultado).isEmpty();
  }

  @Test
  void selecionarPaginas_ordenaHierarquicamentePaiAntesDoFilho() throws Exception {
    Pagina pai = paginaPublicada("Configurações", "config", 1);
    Pagina filho = paginaPublicadaComPai("Perfil", "perfil", 1, pai);
    when(clienteModuloRepository.findModuloIdsByClienteId(clienteId))
        .thenReturn(List.of(modulo.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(filho, pai));

    List<Pagina> resultado = service.selecionarPaginas(clienteId);

    assertThat(resultado).containsExactly(pai, filho);
  }

  @Test
  void selecionarPaginas_comVinculoDiretoNoFilho_incluiPaiPublicado() throws Exception {
    Pagina pai = paginaPublicada("Operações", "operacoes", 0);
    Pagina filho = paginaPublicadaComPai("Lista", "lista", 1, pai);
    when(clientePaginaRepository.findPaginaIdsByClienteId(clienteId))
        .thenReturn(List.of(filho.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(pai, filho));

    List<Pagina> resultado = service.selecionarPaginas(clienteId);

    assertThat(resultado).containsExactly(pai, filho);
  }

  @Test
  void previewHtml_falhaSemPaginas() {
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.previewHtml(cliente, "1.0.0"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("páginas");
  }

  @Test
  void previewHtml_renderizaTituloVersaoEQuantidade() throws Exception {
    Pagina p = paginaPublicada("Login", "login", 1);
    when(clienteModuloRepository.findModuloIdsByClienteId(clienteId))
        .thenReturn(List.of(modulo.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(p));

    String html = service.previewHtml(cliente, "2.5.0");

    assertThat(html).contains("2.5.0");
    assertThat(html).contains("Login");
    assertThat(html).contains("ACME");
  }

  @Test
  void gerar_falhaSemPaginas() {
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.gerar(cliente, "1.0.0"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void previewPagina_usaRenderizadorEditorialDoManual() throws Exception {
    Pagina pagina = paginaPublicada("Cadastro", "cadastro", 1);

    String html = service.previewPagina(pagina);

    assertThat(html)
        .contains("docflow-manual-layout", GeradorPacoteService.MANUAL_ASSETS_REVISION)
        .contains("class=\"article-body\"", "conteúdo Cadastro")
        .contains(".article-body .objective-card", ".article-body .flow-strip");
  }

  @Test
  void gerar_produzZipComArquivosObrigatoriosEHashEstavel() throws Exception {
    Pagina p = paginaPublicada("Login", "login", 1);
    when(clienteModuloRepository.findModuloIdsByClienteId(clienteId))
        .thenReturn(List.of(modulo.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(p));

    ResultadoGeracao resultado = service.gerar(cliente, "1.0.0");

    assertThat(resultado.arquivoZipNome()).isEqualTo("manual-acme-v1.0.0.zip");
    assertThat(resultado.quantidadePaginas()).isEqualTo(1);
    assertThat(resultado.quantidadeModulos()).isEqualTo(1);
    assertThat(resultado.hashPacote()).matches("^[0-9a-f]{64}$");
    assertThat(resultado.relatorioValidacaoJson()).contains("\"validacaoBasicaOk\" : true");

    Path zip = Path.of(resultado.arquivoZipCaminho());
    assertThat(zip).exists();
    try (ZipFile zf = new ZipFile(zip.toFile())) {
      java.util.Set<String> entries = new java.util.LinkedHashSet<>();
      var it = zf.entries();
      while (it.hasMoreElements()) {
        entries.add(it.nextElement().getName());
      }
      assertThat(entries).contains(
          "index.html", "versao.html", "routes.json", "search-index.json",
          "manifest.json", "manifest.webmanifest", "sw.js",
          "assets/app.css", "assets/app.js",
          "paginas/login.html");
      String css = new String(zf.getInputStream(zf.getEntry("assets/app.css")).readAllBytes(),
          java.nio.charset.StandardCharsets.UTF_8);
      assertThat(css)
          .contains("docflow-manual layout-v19", ".article-body .doc-intro", ".article-body .steps>ol", "--accent:#4f46e5")
          .contains(".article-body .objective-card", ".article-body .screen-grid", ".article-body .flow-strip")
          .contains(".article-body .journey-grid", ".article-body .resource-list", ".article-body .status-list")
          .contains("nav a.active", "nav-toggle", ".status-badge--sim", ".condition-stack", "callout--danger", "content:'✓'");
      String appJs = new String(zf.getInputStream(zf.getEntry("assets/app.js")).readAllBytes(),
          java.nio.charset.StandardCharsets.UTF_8);
      assertThat(appJs).contains("manualFilterNav", "nav-toggle", "data-codigo-tela", "manualOpenByCodigoTela");
      String index = new String(zf.getInputStream(zf.getEntry("index.html")).readAllBytes(),
          java.nio.charset.StandardCharsets.UTF_8);
      assertThat(index).contains("CENTRAL DE AJUDA", "Olá! Como podemos ajudar?", "id=\"welcome-search\"")
          .contains("data-welcome-page", "Encontre a resposta certa");
    }
  }

  @Test
  void gerar_marcaPaginaAtivaEMenuColapsavelNaHierarquia() throws Exception {
    Pagina pai = paginaPublicada("Operações", "operacoes", 0);
    Pagina filho = paginaPublicadaComPai("Lista", "lista", 0, pai);
    when(clienteModuloRepository.findModuloIdsByClienteId(clienteId))
        .thenReturn(List.of(modulo.getId()));
    when(paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO))
        .thenReturn(List.of(pai, filho));

    ResultadoGeracao resultado = service.gerar(cliente, "1.0.0");
    Path zip = Path.of(resultado.arquivoZipCaminho());
    try (ZipFile zf = new ZipFile(zip.toFile())) {
      String lista = new String(zf.getInputStream(zf.getEntry("paginas/lista.html")).readAllBytes(),
          java.nio.charset.StandardCharsets.UTF_8);
      assertThat(lista)
          .contains("data-slug=\"lista\"")
          .contains("class=\"active\"")
          .contains("aria-current=\"page\"")
          .contains("has-children is-open")
          .contains("nav-toggle");
      String operacoes = new String(zf.getInputStream(zf.getEntry("paginas/operacoes.html")).readAllBytes(),
          java.nio.charset.StandardCharsets.UTF_8);
      assertThat(operacoes).contains("data-slug=\"operacoes\"").contains("class=\"active\"");
    }
  }

  private Pagina paginaPublicada(String titulo, String slug, int ordem) throws Exception {
    return paginaPublicadaComPai(titulo, slug, ordem, null);
  }

  private Pagina paginaPublicadaComPai(String titulo, String slug, int ordem, Pagina parent) throws Exception {
    Pagina p = new Pagina(titulo, slug, slug.toUpperCase(), "resumo",
        "<p>conteúdo " + titulo + "</p>", ordem, true, modulo, parent);
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
