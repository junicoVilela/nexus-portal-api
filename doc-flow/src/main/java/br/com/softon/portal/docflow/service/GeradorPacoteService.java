package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.repository.ClienteModuloRepository;
import br.com.softon.portal.docflow.repository.ClientePaginaRepository;
import br.com.softon.portal.docflow.service.EmpresaLogoService;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.PaginaAnexo;
import br.com.softon.portal.docflow.entity.StatusPagina;
import br.com.softon.portal.docflow.repository.PaginaAnexoRepository;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.shared.config.StorageProperties;
import br.com.softon.portal.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class GeradorPacoteService {

  /** Revisão dos assets estáticos do manual ZIP; aparece no comentário do app.css, na meta do HTML e na query string (cache bust). */
  public static final String MANUAL_ASSETS_REVISION = "layout-v9";

  private final ClienteModuloRepository clienteModuloRepository;
  private final ClientePaginaRepository clientePaginaRepository;
  private final br.com.softon.portal.docflow.repository.ClienteProjetoRepository clienteProjetoRepository;
  private final PaginaRepository paginaRepository;
  private final PaginaAnexoRepository paginaAnexoRepository;
  private final StorageProperties storageProperties;
  private final EmpresaLogoService empresaLogoService;
  private final ObjectMapper objectMapper;

  public GeradorPacoteService(ClienteModuloRepository clienteModuloRepository,
      ClientePaginaRepository clientePaginaRepository, PaginaRepository paginaRepository,
      br.com.softon.portal.docflow.repository.ClienteProjetoRepository clienteProjetoRepository,
      PaginaAnexoRepository paginaAnexoRepository,
      StorageProperties storageProperties,
      EmpresaLogoService empresaLogoService,
      ObjectMapper objectMapper) {
    this.clienteModuloRepository = clienteModuloRepository;
    this.clientePaginaRepository = clientePaginaRepository;
    this.clienteProjetoRepository = clienteProjetoRepository;
    this.paginaRepository = paginaRepository;
    this.paginaAnexoRepository = paginaAnexoRepository;
    this.storageProperties = storageProperties;
    this.empresaLogoService = empresaLogoService;
    this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
  }

  public ResultadoGeracao gerar(Cliente cliente, String versao) throws IOException {
    List<Pagina> paginas = selecionarPaginas(cliente.getId());
    if (paginas.isEmpty()) {
      throw new BusinessException("Não há páginas publicadas elegíveis para este cliente.");
    }

    String pacoteNome = "manual-%s-v%s".formatted(cliente.getSlug(), versao);
    Path storageDir = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize();
    Path workDir = storageDir.resolve("tmp").resolve(pacoteNome);
    Path zipPath = storageDir.resolve(pacoteNome + ".zip");

    FileSystemUtils.deleteRecursively(workDir);
    Files.createDirectories(workDir.resolve("paginas"));
    Files.createDirectories(workDir.resolve("assets"));
    Files.createDirectories(storageDir);

    escreverAssets(workDir.resolve("assets"));
    escreverLogos(cliente, workDir.resolve("assets"));
    escreverPaginas(cliente, versao, paginas, workDir);
    escreverJson(cliente, versao, paginas, workDir);
    escreverIndex(cliente, versao, paginas, workDir);
    escreverSobreVersao(cliente, versao, paginas, workDir);
    escreverPwa(cliente, versao, paginas, workDir);

    Files.deleteIfExists(zipPath);
    zipDirectory(workDir, zipPath);
    FileSystemUtils.deleteRecursively(workDir);

    Map<String, Object> relatorioValidacao =
        montarRelatorioValidacaoPacote(zipPath, paginas);
    aplicarOuFalharValidacaoPacote(zipPath, relatorioValidacao);

    String hash = sha256(zipPath);
    relatorioValidacao.put("sha256Pacote", hash);

    String relatorioJson = objectMapper.writeValueAsString(relatorioValidacao);

    long modulos = paginas.stream().map(p -> p.getModulo().getId()).distinct().count();
    return new ResultadoGeracao(zipPath.getFileName().toString(), zipPath.toString(), hash,
        paginas.size(), (int) modulos, relatorioJson);
  }

  @SuppressWarnings("unchecked")
  private void aplicarOuFalharValidacaoPacote(Path zipPath, Map<String, Object> relatorio)
      throws IOException {
    List<String> faltantes = (List<String>) relatorio.getOrDefault(
        "arquivosObrigatoriosFaltantes", Collections.emptyList());
    if (!faltantes.isEmpty()) {
      Files.deleteIfExists(zipPath);
      throw new BusinessException("Pacote rejeitado na validação. Faltando: "
          + String.join(", ", faltantes));
    }
  }

  private Map<String, Object> montarRelatorioValidacaoPacote(Path zipPath, List<Pagina> paginas)
      throws IOException {
    Map<String, Object> doc = new LinkedHashMap<>();
    long tamanho = Files.exists(zipPath) ? Files.size(zipPath) : 0;
    doc.put("tamanhoBytes", tamanho);

    List<String> faltantes = new ArrayList<>();
    List<String> avisos = new ArrayList<>();

    if (tamanho < 200) {
      faltantes.add("(pacote_zip_muito_pequeno)");
    }

    Set<String> present = new LinkedHashSet<>();
    if (Files.exists(zipPath)) {
      try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipPath.toFile())) {
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
          present.add(entries.nextElement().getName().replace('\\', '/'));
        }
      }
    }

    Set<String> obrigatorios = new LinkedHashSet<>(List.of(
        "index.html",
        "versao.html",
        "routes.json",
        "search-index.json",
        "manifest.json",
        "manifest.webmanifest",
        "sw.js",
        "assets/app.css",
        "assets/app.js"));
    for (String req : obrigatorios) {
      if (!present.contains(req)) {
        faltantes.add(req);
      }
    }

    for (Pagina p : paginas) {
      String pathHtml = "paginas/" + p.getSlug() + ".html";
      if (!present.contains(pathHtml)) {
        faltantes.add(pathHtml);
      }
    }

    int htmlNoPacote = (int) present.stream()
        .filter(n -> n.startsWith("paginas/") && n.endsWith(".html"))
        .count();
    if (htmlNoPacote != paginas.size()) {
      avisos.add("Quantidade de HTML em paginas/ (%d) difere do esperado (%d)."
          .formatted(htmlNoPacote, paginas.size()));
    }

    doc.put("entradasNoZip", present.size());
    doc.put("arquivosObrigatoriosFaltantes", faltantes);
    doc.put("avisos", avisos);
    doc.put("validacaoBasicaOk", faltantes.isEmpty());
    return doc;
  }

  private void escreverLogos(Cliente cliente, Path assetsDir) throws IOException {
    // Logo do cliente (esquerda)
    if (cliente.getLogoPath() != null) {
      Path src = Path.of(cliente.getLogoPath());
      if (Files.exists(src)) {
        String ext = extensao(src.getFileName().toString());
        Files.copy(src, assetsDir.resolve("logo-cliente" + ext), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    }
    // Logo da empresa (direita)
    empresaLogoService.encontrar().ifPresent(src -> {
      try {
        String ext = extensao(src.getFileName().toString());
        Files.copy(src, assetsDir.resolve("logo-empresa" + ext), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException ex) {
        // Logo da empresa não-crítico: continua sem ela
      }
    });
  }

  public List<Pagina> selecionarPaginas(UUID clienteId) {
    Set<UUID> modulosDoCliente = new LinkedHashSet<>(clienteModuloRepository.findModuloIdsByClienteId(clienteId));
    Set<UUID> paginasDiretas = new LinkedHashSet<>(clientePaginaRepository.findPaginaIdsByClienteId(clienteId));
    Set<UUID> projetosDoCliente = new LinkedHashSet<>(clienteProjetoRepository.findProjetoIdsByClienteId(clienteId));

    return paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO).stream()
        .filter(pagina -> projetosDoCliente.contains(pagina.getModulo().getProjeto().getId())
            || modulosDoCliente.contains(pagina.getModulo().getId())
            || paginasDiretas.contains(pagina.getId()))
        .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),
            this::ordenarHierarquia));
  }

  public String previewHtml(Cliente cliente, String versao) {
    List<Pagina> paginas = selecionarPaginas(cliente.getId());
    if (paginas.isEmpty()) {
      throw new BusinessException("Não há páginas publicadas elegíveis para este cliente.");
    }
    String menu = previewMenuHtml(paginas);
    String clientePreviewLogo = buildClienteLogoHtmlPreview(cliente);
    StringBuilder content = new StringBuilder();
    for (int index = 0; index < paginas.size(); index++) {
      Pagina pagina = paginas.get(index);
      content.append("""
          <section id="pagina-%d" class="page %s">
            <div class="page-client">%s</div>
            <header>
              <span>%s</span>
              <h1>%s</h1>
              %s
            </header>
            <div class="article-content">%s</div>
          </section>
          """.formatted(index + 1, index == 0 ? "active" : "", clientePreviewLogo,
          HtmlUtils.htmlEscape(breadcrumb(pagina)),
          HtmlUtils.htmlEscape(pagina.getTitulo()),
          pagina.getResumo() == null || pagina.getResumo().isBlank()
              ? ""
              : "<p>" + HtmlUtils.htmlEscape(pagina.getResumo()) + "</p>",
          pagina.getConteudoHtml() == null || pagina.getConteudoHtml().isBlank()
              ? "<p>Sem conteúdo HTML cadastrado.</p>"
              : pagina.getConteudoHtml()));
    }
    return previewTemplate(cliente, versao, paginas.size(), menu, content.toString());
  }

  private void escreverPaginas(Cliente cliente, String versao, List<Pagina> paginas, Path workDir) throws IOException {
    String menu = menuHtml(paginas, "");
    for (Pagina pagina : paginas) {
      String conteudo = prepararConteudoComAnexos(pagina, workDir.resolve("assets"), "../assets");
      String html = template(cliente, versao, pagina.getTitulo(), breadcrumb(pagina), menu,
          conteudo, "../assets");
      Files.writeString(workDir.resolve("paginas").resolve(pagina.getSlug() + ".html"), html);
    }
  }

  private String prepararConteudoComAnexos(Pagina pagina, Path assetsDir, String assetBase) throws IOException {
    String conteudo = pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml();
    List<PaginaAnexo> anexos = paginaAnexoRepository.findByPagina_Id(pagina.getId());
    if (anexos.isEmpty() || conteudo.isBlank()) {
      return conteudo;
    }

    org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(conteudo);
    Path paginaAssetsDir = assetsDir.resolve("anexos").resolve(pagina.getSlug());
    for (PaginaAnexo anexo : anexos) {
      Path origem = Path.of(anexo.getCaminho());
      if (!Files.exists(origem)) {
        continue;
      }
      String filename = anexo.getId() + extensao(anexo.getNomeOriginal());
      Files.createDirectories(paginaAssetsDir);
      Files.copy(origem, paginaAssetsDir.resolve(filename), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      String staticSrc = assetBase + "/anexos/" + pagina.getSlug() + "/" + filename;
      document.select("img[src]").forEach(img -> {
        String src = img.attr("src");
        if (src.contains(anexo.getId().toString())) {
          img.attr("src", staticSrc);
        }
      });
    }
    return document.body().html();
  }

  private String extensao(String filename) {
    int dot = filename == null ? -1 : filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) {
      return ".bin";
    }
    return filename.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase(java.util.Locale.ROOT);
  }

  private void escreverJson(Cliente cliente, String versao, List<Pagina> paginas, Path workDir) throws IOException {
    Map<String, String> routes = new LinkedHashMap<>();
    List<Map<String, String>> searchIndex = paginas.stream()
        .map(pagina -> {
          String url = "paginas/" + pagina.getSlug() + ".html";
          routes.put(pagina.getCodigoTela(), url);
          return Map.of(
              "titulo", pagina.getTitulo(),
              "url", url,
              "codigoTela", pagina.getCodigoTela(),
              "texto", textoBusca(pagina));
        })
        .toList();

    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("cliente", cliente.getNome());
    manifest.put("slugCliente", cliente.getSlug());
    manifest.put("versao", versao);
    manifest.put("quantidadePaginas", paginas.size());
    manifest.put("geradoEm", OffsetDateTime.now().toString());
    manifest.put("temaCorPrimaria", cliente.getTemaCorPrimariaOuPadrao());
    manifest.put("temaCorFundo", cliente.getTemaCorFundoOuPadrao());

    objectMapper.writeValue(workDir.resolve("routes.json").toFile(), routes);
    objectMapper.writeValue(workDir.resolve("search-index.json").toFile(), searchIndex);
    objectMapper.writeValue(workDir.resolve("manifest.json").toFile(), manifest);
  }

  private void escreverIndex(Cliente cliente, String versao, List<Pagina> paginas, Path workDir) throws IOException {
    String cards = paginas.stream()
        .map(p -> """
            <a class="page-card" href="paginas/%s.html">
              <span>%s</span>
              <strong>%s</strong>
              <small>%s</small>
            </a>
            """.formatted(p.getSlug(), HtmlUtils.htmlEscape(p.getModulo().getNome()),
            HtmlUtils.htmlEscape(p.getTitulo()), HtmlUtils.htmlEscape(p.getCodigoTela())))
        .reduce("", String::concat);
    cards = """
        <a class="page-card" href="versao.html">
          <span>Publicação</span>
          <strong>Sobre esta versão</strong>
          <small>Escopo, data e páginas incluídas</small>
        </a>
        """ + cards;
    Files.writeString(workDir.resolve("index.html"), template(cliente, versao,
        "Manual " + cliente.getNome(), "Início", menuHtml(paginas, "paginas/"),
        "<div class=\"cards\">" + cards + "</div>", "assets"));
  }

  private void escreverSobreVersao(Cliente cliente, String versao, List<Pagina> paginas, Path workDir) throws IOException {
    String linhas = paginas.stream()
        .map(pagina -> """
            <tr>
              <td>%s</td>
              <td>%s</td>
              <td>%s</td>
            </tr>
            """.formatted(HtmlUtils.htmlEscape(pagina.getModulo().getNome()),
            HtmlUtils.htmlEscape(pagina.getTitulo()), HtmlUtils.htmlEscape(pagina.getCodigoTela())))
        .reduce("", String::concat);
    String content = """
        <h1>Sobre esta versão</h1>
        <p>Manual gerado para <strong>%s</strong> na versão <strong>%s</strong>.</p>
        <p>Gerado em %s com %d páginas publicadas.</p>
        <h2>Páginas incluídas</h2>
        <table>
          <thead><tr><th>Módulo</th><th>Página</th><th>Código da tela</th></tr></thead>
          <tbody>%s</tbody>
        </table>
        """.formatted(HtmlUtils.htmlEscape(cliente.getNome()), HtmlUtils.htmlEscape(versao),
        HtmlUtils.htmlEscape(OffsetDateTime.now().toString()), paginas.size(), linhas);
    Files.writeString(workDir.resolve("versao.html"), template(cliente, versao, "Sobre esta versão",
        "Publicação / Sobre esta versão", menuHtml(paginas, "paginas/"), content, "assets"));
  }

  private String previewTemplate(Cliente cliente, String versao, int quantidadePaginas, String menu, String content) {
    boolean temLogoEmpresa = empresaLogoService.encontrar().isPresent();
    String logoEmpresaUrl = temLogoEmpresa ? "/api/empresa/logo" : "";

    String empresaBrandBlock;
    if (temLogoEmpresa) {
      empresaBrandBlock = "<div class=\"brand-panel\"><div class=\"brand\"><img class=\"brand-logo-empresa\" src=\""
          + logoEmpresaUrl + "\" alt=\"empresa\"></div>"
          + "<p class=\"preview-versao\">Versão " + HtmlUtils.htmlEscape(versao)
          + " · " + quantidadePaginas + " páginas</p></div>";
    } else {
      empresaBrandBlock = "<div class=\"brand-panel\"><div class=\"brand brand-text\">DocFlow</div>"
          + "<p class=\"preview-versao\">Versão " + HtmlUtils.htmlEscape(versao)
          + " · " + quantidadePaginas + " páginas</p></div>";
    }

    return """
        <!doctype html>
        <html lang="pt-BR">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Prévia do manual - %s</title>
          <link rel="preconnect" href="https://fonts.googleapis.com">
          <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
          <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,400;0,500;0,700;1,400&amp;display=swap">
          <style>
            :root{%s}
            *{box-sizing:border-box}body{margin:0;color:var(--text);background:var(--bg);font-family:var(--font-ui);font-size:14px;-webkit-font-smoothing:antialiased}
            .shell{display:grid;grid-template-columns:260px minmax(0,1fr);grid-template-rows:1fr;min-height:100vh;width:100%;align-items:stretch}
            aside{background:var(--surface);color:var(--text);border-right:1px solid var(--border);padding:20px 12px;position:sticky;top:0;height:100vh;overflow:auto;box-shadow:1px 0 0 rgba(0,0,0,.06)}
            .brand-panel{margin:-20px -12px 16px -12px;padding:16px 12px 14px;background:var(--surface);border-bottom:1px solid var(--border)}
            .brand{display:flex;align-items:center;justify-content:center;padding:4px 0}
            .brand-logo-empresa{width:100%;max-height:56px;object-fit:contain}
            .brand-text{font-weight:700;font-size:18px;color:var(--accent);justify-content:center;letter-spacing:-0.01em}
            .preview-versao{color:var(--muted);font-size:12px;line-height:16px;margin:10px 0 0;text-align:center;font-weight:400}
            aside nav{margin-top:4px}
            aside ul{list-style:none;margin:0;padding:0}aside li{margin:2px 0}
            aside a{display:grid;gap:2px;color:#3c4043;text-decoration:none;padding:10px 16px;border-radius:999px;font-size:14px;font-weight:400;line-height:20px;transition:background .15s,color .15s}
            aside a:hover{background:#f1f3f4;color:#202124}
            aside a.active{background:var(--accent-soft);color:#1967d2;font-weight:500}
            aside a span{color:var(--muted);font-size:12px;line-height:16px;font-weight:400}
            main{padding:24px clamp(16px,2.5vw,28px) 32px;width:100%;min-width:0;max-width:none;margin:0;display:flex;flex-direction:column;align-self:stretch}
            .page-client{display:flex;justify-content:flex-end;align-items:center;padding:14px 28px;background:#fafafa;border-bottom:1px solid var(--border)}
            .cliente-logo{height:48px;width:auto;max-width:220px;object-fit:contain}
            .cliente-nome{font-size:14px;font-weight:500;color:#3c4043;white-space:nowrap;font-family:var(--font-ui)}
            .page{display:none;width:100%;max-width:none;margin:0;background:var(--surface);border:1px solid var(--border);border-radius:8px;overflow:hidden;box-shadow:0 1px 2px 0 rgba(60,64,67,.3),0 2px 6px 2px rgba(60,64,67,.15);flex:1 1 auto;min-width:0}
            .page.active{display:block}.page header{border-bottom:1px solid var(--border);padding:22px 28px 26px}
            .page header span{color:var(--muted);font-size:12px;letter-spacing:.1px;font-family:var(--font-ui)}.page h1{margin:8px 0 0;font-size:clamp(1.35rem,2.1vw,1.625rem);font-weight:600;line-height:1.25;font-family:var(--font-body);color:var(--text)}
            .page p{line-height:1.65;font-family:var(--font-body)}.article-content{padding:28px 28px 34px;line-height:1.65;font-family:var(--font-body);font-size:15px;color:#3c4043}.article-content :first-child{margin-top:0}.article-content :last-child{margin-bottom:0}
            .article-content a{color:var(--accent)}
            .article-content img{max-width:100%;height:auto;display:block;border-radius:8px;margin:12px 0}
            .article-content figure.photo{margin:12px 0}.article-content figure.photo figcaption{font-size:12px;color:var(--muted);margin-top:6px;font-family:var(--font-ui)}
            @media(max-width:900px){.shell{display:block;grid-template-columns:1fr}aside{position:static;height:auto}main{padding:16px}}
          </style>
        </head>
        <body>
          <div class="shell">
            <aside>
              %s
              <nav>%s</nav>
            </aside>
            <main>%s</main>
          </div>
          <script>
            const links=Array.from(document.querySelectorAll('aside a[data-page]'));
            const pages=Array.from(document.querySelectorAll('main .page'));
            function openPage(id,updateUrl=true){
              links.forEach(link=>link.classList.toggle('active',link.dataset.page===id));
              pages.forEach(page=>page.classList.toggle('active',page.id===id));
              const page=document.getElementById(id);
              if(page){
                document.title=page.querySelector('h1')?.textContent||document.title;
                if(updateUrl&&location.hash!=='#'+id)location.hash=id;
                window.scrollTo({top:0,behavior:'smooth'});
              }
            }
            window.addEventListener('hashchange',()=>{const id=location.hash.slice(1);if(id)openPage(id,false);});
            links.forEach(link=>link.addEventListener('click',event=>{event.preventDefault();openPage(link.dataset.page);}));
            const initial=location.hash?location.hash.slice(1):links[0]?.dataset.page;
            if(initial)openPage(initial,!location.hash);
          </script>
        </body>
        </html>
        """.formatted(HtmlUtils.htmlEscape(cliente.getNome()), cssVariaveisRaizPreviewCliente(cliente),
        empresaBrandBlock, menu, content);
  }

  private String cssVariaveisRaizPreviewCliente(Cliente cliente) {
    String ac = cliente.getTemaCorPrimariaOuPadrao();
    String bg = cliente.getTemaCorFundoOuPadrao();
    String hover = ac.equalsIgnoreCase("#1a73e8") ? "#174ea6" : ac;
    return "--font-ui:Roboto,system-ui,\"Segoe UI\",Arial,sans-serif;"
        + "--font-body:Roboto,system-ui,\"Segoe UI\",Arial,sans-serif;"
        + "--border:#dadce0;--bg:%s;--surface:#fff;--text:#202124;--muted:#5f6368;"
        + "--accent:%s;--accent-hover:%s;--accent-soft:#e8f0fe".formatted(bg, ac, hover);
  }

  private String temaStyleOverridePacoteHtml(Cliente cliente) {
    String ac = cliente.getTemaCorPrimariaOuPadrao();
    String bg = cliente.getTemaCorFundoOuPadrao();
    String hover = ac.equalsIgnoreCase("#1a73e8") ? "#174ea6" : ac;
    return "<style>:root{--accent:%s!important;--accent-hover:%s!important;--bg:%s!important;--accent-soft:#e8f0fe!important}</style>"
        .formatted(ac, hover, bg);
  }

  private String template(Cliente cliente, String versao, String title, String breadcrumb, String menu, String content,
      String assetBase) {
    String brandHtml = buildEmpresaBrandHtml(versao, assetBase);
    String clienteLogoHtml = buildClienteLogoHtml(cliente, assetBase);
    String assetQuery =
        "rev=" + MANUAL_ASSETS_REVISION + "&ver=" + URLEncoder.encode(versao, StandardCharsets.UTF_8);
    String temaTag = temaStyleOverridePacoteHtml(cliente);
    String temaMetaCor = HtmlUtils.htmlEscape(cliente.getTemaCorPrimariaOuPadrao());
    return """
        <!doctype html>
        <html lang="pt-BR">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <link rel="preconnect" href="https://fonts.googleapis.com">
          <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
          <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,400;0,500;0,700;1,400&amp;display=swap">
          <meta name="docflow-manual-layout" content="%s">
          <meta name="theme-color" content="%s">
          <title>%s</title>
          <link rel="manifest" href="%s/../manifest.webmanifest">
          <link rel="stylesheet" href="%s/app.css?%s">
          %s
          <script defer src="%s/app.js?%s"></script>
        </head>
        <body>
          <aside class="sidebar">
            %s
            <input id="manual-search" type="search" placeholder="Buscar no manual">
            <nav>%s</nav>
          </aside>
          <main class="content">
            <div class="breadcrumb-bar">
              <div class="breadcrumb">%s</div>
            </div>
            <article>
              <header class="article-head">%s</header>
              <div class="article-body">%s</div>
            </article>
          </main>
        </body>
        </html>
        """.formatted(HtmlUtils.htmlEscape(MANUAL_ASSETS_REVISION), temaMetaCor, HtmlUtils.htmlEscape(title),
        assetBase, assetBase, assetQuery, temaTag, assetBase, assetQuery, brandHtml, menu,
        HtmlUtils.htmlEscape(breadcrumb), clienteLogoHtml, content);
  }

  /** Logo da empresa ocupa toda a largura da sidebar (painel destacado). */
  private String buildEmpresaBrandHtml(String versao, String assetBase) {
    boolean temLogoEmpresa = empresaLogoService.encontrar().isPresent();
    String versaoEsc = HtmlUtils.htmlEscape(versao);
    if (temLogoEmpresa) {
      String ext = extensao(empresaLogoService.encontrar().get().getFileName().toString());
      return "<div class=\"brand-panel\"><div class=\"brand\"><img class=\"brand-logo-empresa\" src=\""
          + assetBase + "/logo-empresa" + ext + "\" alt=\"empresa\"></div>"
          + "<small class=\"brand-versao\">v" + versaoEsc + "</small></div>";
    }
    return "<div class=\"brand-panel\"><div class=\"brand brand-text\">DocFlow <small>v" + versaoEsc + "</small></div></div>";
  }

  /** Logo do cliente no topo do card de conteúdo (direita). */
  private String buildClienteLogoHtml(Cliente cliente, String assetBase) {
    boolean temLogoCliente = cliente.getLogoPath() != null
        && java.nio.file.Files.exists(java.nio.file.Path.of(cliente.getLogoPath()));
    if (temLogoCliente) {
      String ext = extensao(java.nio.file.Path.of(cliente.getLogoPath()).getFileName().toString());
      return "<img class=\"cliente-logo\" src=\"" + assetBase + "/logo-cliente" + ext
          + "\" alt=\"" + HtmlUtils.htmlEscape(cliente.getNome()) + "\">";
    }
    return "<span class=\"cliente-nome\">" + HtmlUtils.htmlEscape(cliente.getNome()) + "</span>";
  }

  private String buildClienteLogoHtmlPreview(Cliente cliente) {
    boolean temLogoCliente = cliente.getLogoPath() != null
        && java.nio.file.Files.exists(java.nio.file.Path.of(cliente.getLogoPath()));
    if (temLogoCliente) {
      return "<img class=\"cliente-logo\" src=\"/api/clientes/" + cliente.getId() + "/logo\" alt=\""
          + HtmlUtils.htmlEscape(cliente.getNome()) + "\">";
    }
    return "<span class=\"cliente-nome\">" + HtmlUtils.htmlEscape(cliente.getNome()) + "</span>";
  }

  private String menuHtml(List<Pagina> paginas, String pagePrefix) {
    Map<String, List<Pagina>> grupos = new LinkedHashMap<>();
    for (Pagina pagina : paginas) {
      grupos.computeIfAbsent(pagina.getModulo().getNome(), ignored -> new java.util.ArrayList<>()).add(pagina);
    }
    StringBuilder html = new StringBuilder();
    grupos.forEach((modulo, items) -> {
      html.append("<section><h2>").append(HtmlUtils.htmlEscape(modulo)).append("</h2>");
      appendMenuItems(html, roots(items), filhosPorParent(items), pagePrefix);
      html.append("</section>");
    });
    return html.toString();
  }

  private String previewMenuHtml(List<Pagina> paginas) {
    Map<UUID, Integer> indexById = new LinkedHashMap<>();
    for (int index = 0; index < paginas.size(); index++) {
      indexById.put(paginas.get(index).getId(), index + 1);
    }
    StringBuilder html = new StringBuilder();
    appendPreviewMenuItems(html, roots(paginas), filhosPorParent(paginas), indexById);
    return html.toString();
  }

  private String breadcrumb(Pagina pagina) {
    List<String> parts = new ArrayList<>();
    parts.add(pagina.getModulo().getNome());
    parts.add(0, pagina.getModulo().getProjeto().getNome());
    for (Pagina atual : cadeia(pagina)) {
      parts.add(atual.getTitulo());
    }
    return String.join(" / ", parts);
  }

  private List<Pagina> ordenarHierarquia(List<Pagina> paginas) {
    Map<UUID, List<Pagina>> filhos = filhosPorParent(paginas);
    List<Pagina> ordenadas = new ArrayList<>();
    for (Pagina root : roots(paginas)) {
      appendOrdenadas(root, filhos, ordenadas);
    }
    return ordenadas;
  }

  private void appendOrdenadas(Pagina pagina, Map<UUID, List<Pagina>> filhos, List<Pagina> ordenadas) {
    ordenadas.add(pagina);
    for (Pagina child : filhos.getOrDefault(pagina.getId(), List.of())) {
      appendOrdenadas(child, filhos, ordenadas);
    }
  }

  private List<Pagina> roots(List<Pagina> paginas) {
    Set<UUID> ids = paginas.stream().map(Pagina::getId).collect(java.util.stream.Collectors.toSet());
    return paginas.stream()
        .filter(pagina -> pagina.getParent() == null || !ids.contains(pagina.getParent().getId()))
        .sorted(hierarquiaComparator())
        .toList();
  }

  private Map<UUID, List<Pagina>> filhosPorParent(List<Pagina> paginas) {
    Map<UUID, List<Pagina>> filhos = new LinkedHashMap<>();
    for (Pagina pagina : paginas) {
      if (pagina.getParent() != null) {
        filhos.computeIfAbsent(pagina.getParent().getId(), ignored -> new ArrayList<>()).add(pagina);
      }
    }
    filhos.values().forEach(items -> items.sort(hierarquiaComparator()));
    return filhos;
  }

  private Comparator<Pagina> hierarquiaComparator() {
    return Comparator
        .comparing((Pagina pagina) -> pagina.getModulo().getProjeto().getNome())
        .thenComparing(pagina -> pagina.getModulo().getOrdem())
        .thenComparing(Pagina::getOrdem)
        .thenComparing(Pagina::getTitulo);
  }

  private void appendMenuItems(StringBuilder html, List<Pagina> paginas, Map<UUID, List<Pagina>> filhos,
      String pagePrefix) {
    html.append("<ul>");
    for (Pagina pagina : paginas) {
      html.append("<li><a href=\"").append(pagePrefix).append(pagina.getSlug()).append(".html\" data-codigo-tela=\"")
          .append(HtmlUtils.htmlEscape(pagina.getCodigoTela())).append("\">")
          .append(HtmlUtils.htmlEscape(pagina.getTitulo())).append("</a>");
      List<Pagina> children = filhos.getOrDefault(pagina.getId(), List.of());
      if (!children.isEmpty()) {
        appendMenuItems(html, children, filhos, pagePrefix);
      }
      html.append("</li>");
    }
    html.append("</ul>");
  }

  private void appendPreviewMenuItems(StringBuilder html, List<Pagina> paginas, Map<UUID, List<Pagina>> filhos,
      Map<UUID, Integer> indexById) {
    html.append("<ul>");
    for (Pagina pagina : paginas) {
      int index = indexById.getOrDefault(pagina.getId(), 1);
      html.append("<li><a href=\"#pagina-").append(index).append("\" class=\"")
          .append(index == 1 ? "active" : "").append("\" data-page=\"pagina-").append(index).append("\">")
          .append("<strong>").append(HtmlUtils.htmlEscape(pagina.getTitulo())).append("</strong>")
          .append("<span>").append(HtmlUtils.htmlEscape(pagina.getModulo().getNome()))
          .append(" · ").append(HtmlUtils.htmlEscape(pagina.getCodigoTela())).append("</span></a>");
      List<Pagina> children = filhos.getOrDefault(pagina.getId(), List.of());
      if (!children.isEmpty()) {
        appendPreviewMenuItems(html, children, filhos, indexById);
      }
      html.append("</li>");
    }
    html.append("</ul>");
  }

  private List<Pagina> cadeia(Pagina pagina) {
    List<Pagina> chain = new ArrayList<>();
    for (Pagina atual = pagina; atual != null; atual = atual.getParent()) {
      chain.add(0, atual);
    }
    return chain;
  }

  private String textoBusca(Pagina pagina) {
    return (pagina.getResumo() == null ? "" : pagina.getResumo() + " ")
        + Jsoup.parse(pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml()).text();
  }

  private void escreverAssets(Path assetsDir) throws IOException {
    String cssBanner = """
        /*
         * docflow-manual %s
         * Se este comentário não existir no topo do arquivo, o ZIP não foi gerado por esta revisão do backend.
         */
        """.formatted(MANUAL_ASSETS_REVISION);
    Files.writeString(assetsDir.resolve("app.css"), cssBanner + """
        :root{
        color-scheme:light;
        --font-ui:Roboto,system-ui,-apple-system,"Segoe UI",Arial,sans-serif;
        --font-body:Roboto,system-ui,-apple-system,"Segoe UI",Arial,sans-serif;
        --border:#dadce0;--bg:#f8f9fa;--surface:#fff;--text:#202124;--muted:#5f6368;
        --accent:#1a73e8;--accent-hover:#174ea6;--accent-soft:#e8f0fe;--nav-hover:#f1f3f4;
        --sidebar-w:272px;
        font-family:var(--font-ui);
        }
        *{box-sizing:border-box}html,body{height:100%}
        body{margin:0;display:grid;grid-template-columns:var(--sidebar-w) minmax(0,1fr);grid-template-rows:1fr;min-height:100vh;width:100%;color:var(--text);background:var(--bg);font-family:var(--font-ui);font-size:14px;line-height:1.5;-webkit-font-smoothing:antialiased}
        .sidebar{background:var(--surface);border-right:1px solid var(--border);padding:20px 12px;position:sticky;top:0;align-self:start;height:100vh;max-height:100vh;overflow:auto;box-shadow:1px 0 0 rgba(60,64,67,.08)}
        .brand-panel{margin:-20px -12px 14px -12px;padding:16px 12px 14px;background:var(--surface);border-bottom:1px solid var(--border)}
        .brand{margin-bottom:0;display:flex;align-items:center;justify-content:center;padding:6px 0}
        .brand-logo-empresa{width:100%;max-height:58px;object-fit:contain}
        .brand-text{font-weight:700;font-size:18px;color:var(--accent);justify-content:center;text-align:center;letter-spacing:-0.01em}
        .brand-text small{display:block;font-size:11px;color:var(--muted);font-weight:500;margin-top:4px;letter-spacing:0}
        .brand-versao{display:block;font-size:12px;line-height:16px;color:var(--muted);text-align:center;margin:10px 0 0;font-weight:400}
        .breadcrumb-bar{margin-bottom:16px;width:100%}
        .breadcrumb{color:var(--muted);font-size:13px;line-height:1.43}
        .content{padding:24px clamp(16px,2.5vw,28px) 32px;width:100%;min-width:0;max-width:none;margin:0;display:flex;flex-direction:column;align-self:stretch}
        article{background:var(--surface);border:1px solid var(--border);border-radius:8px;overflow:hidden;box-shadow:0 1px 2px 0 rgba(60,64,67,.3),0 2px 6px 2px rgba(60,64,67,.15);width:100%;max-width:none;margin:0;flex:1 1 auto;min-width:0}
        .article-head{display:flex;justify-content:flex-end;align-items:center;min-height:56px;padding:14px 28px;background:#fafafa;border-bottom:1px solid var(--border)}
        .cliente-logo{height:48px;width:auto;max-width:220px;object-fit:contain}
        .cliente-nome{font-size:14px;font-weight:500;color:#3c4043;white-space:nowrap;font-family:var(--font-ui)}
        .article-body{padding:28px clamp(22px,3vw,40px) 36px;line-height:1.65;max-width:none;font-family:var(--font-body);font-size:15px;color:#3c4043}
        .article-body a{color:var(--accent);text-decoration:none;font-weight:500}
        .article-body a:hover{color:var(--accent-hover);text-decoration:underline}
        .article-body table{width:100%;border-collapse:collapse;font-size:14px}.article-body th,.article-body td{border:1px solid var(--border);padding:12px 14px;text-align:left}
        .article-body th{background:#fafafa;font-family:var(--font-ui);font-weight:500;font-size:13px;color:var(--muted)}
        .article-body .callout{border-left:4px solid var(--accent);background:var(--accent-soft);padding:14px 16px;margin:18px 0;border-radius:0 8px 8px 0;color:#174ea6}
        .article-body .warning{border-left:4px solid #ea8600;background:#fef7e0;padding:14px 16px;margin:18px 0;border-radius:0 8px 8px 0;color:#b06000}
        .article-body pre,.article-body code{font-family:ui-monospace,"Roboto Mono",Menlo,monospace}
        .article-body pre{overflow:auto;background:#202124;color:#e8eaed;padding:16px 18px;border-radius:8px;font-size:13px;line-height:1.55}
        .article-body img{max-width:100%;height:auto;display:block;border-radius:8px;margin:16px 0}
        .article-body figure.photo{margin:16px 0}.article-body figure.photo figcaption{font-size:12px;color:var(--muted);margin-top:8px;font-family:var(--font-ui)}
        .article-body h1,.article-body h2,.article-body h3{font-family:var(--font-body);font-weight:600;color:var(--text);line-height:1.3;margin:1.35em 0 .55em;letter-spacing:-0.01em}
        .article-body h1{font-size:1.5rem}.article-body h2{font-size:1.25rem}.article-body h3{font-size:1.0625rem}
        .article-body h1:first-child,.article-body h2:first-child{margin-top:0}
        .article-body p{margin:0 0 1em}.article-body li{margin:.3em 0}
        .cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px}
        .page-card{display:block;border:1px solid var(--border);border-radius:8px;padding:16px 18px;color:inherit;text-decoration:none;background:var(--surface);transition:box-shadow .2s;border-color:var(--border)}
        .page-card:hover{box-shadow:0 1px 2px 0 rgba(60,64,67,.3),0 2px 6px 2px rgba(60,64,67,.15)}
        .page-card span,.page-card small{display:block;color:var(--muted);font-size:12px;font-family:var(--font-ui)}.page-card strong{display:block;margin:8px 0 4px;font-size:15px;color:var(--text);font-family:var(--font-ui);font-weight:600}
        #manual-search{width:100%;height:40px;border:1px solid var(--border);border-radius:8px;padding:0 14px;margin-bottom:12px;font-size:14px;font-family:var(--font-ui);background:var(--surface);color:var(--text)}
        #manual-search:focus{outline:none;border-color:var(--accent);box-shadow:0 0 0 2px rgba(26,115,232,.25)}
        .search-results{display:grid;gap:8px;margin:0 0 16px}
        .search-result{display:block;border:1px solid var(--border);border-radius:8px;padding:10px 14px;color:inherit;text-decoration:none;background:var(--surface)}
        .search-result:hover{border-color:var(--accent);background:var(--accent-soft)}
        .search-result strong{display:block;font-size:14px;font-family:var(--font-ui);font-weight:500}.search-result span{display:block;color:var(--muted);font-size:12px;margin-top:4px}
        nav section{margin:0 0 20px}
        nav h2{font-size:12px;font-weight:500;line-height:16px;color:var(--muted);text-transform:none;letter-spacing:.1px;margin:0 0 8px 16px}
        nav ul{list-style:none;margin:0;padding:0}nav section>ul{padding-left:0}nav li{margin:0}
        nav ul ul{margin:4px 0 0;padding-left:10px;border-left:2px solid #e8eaed}
        nav a{display:block;color:#3c4043;text-decoration:none;padding:10px 16px;border-radius:999px;font-size:14px;font-weight:400;line-height:20px;transition:background .12s ease,color .12s ease}
        nav a:hover{background:var(--nav-hover);color:var(--text)}
        @media(max-width:820px){body{display:block;grid-template-columns:1fr}.sidebar{height:auto;position:relative;max-height:none;box-shadow:none}.content{padding:18px}.article-body{padding:22px}article{border-radius:8px}}
        """);
    Files.writeString(assetsDir.resolve("app.js"), """
        const manualRootBase = window.location.pathname.includes('/paginas/') ? '..' : '.';
        async function manualOpenByCodigoTela(codigoTela, basePath = manualRootBase) {
          const response = await fetch(`${basePath}/routes.json`);
          const routes = await response.json();
          const url = routes[codigoTela];
          if (url) window.open(`${basePath}/${url}`, '_blank');
          return url;
        }
        window.manualOpenByCodigoTela = manualOpenByCodigoTela;
        function manualNormalize(text) {
          return (text || '').toString().normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase();
        }
        function manualSnippet(text, term) {
          const normalizedText = manualNormalize(text);
          const normalizedTerm = manualNormalize(term);
          const index = normalizedText.indexOf(normalizedTerm);
          const start = Math.max(0, index - 42);
          return (text || '').slice(start, start + 118).trim();
        }
        async function manualLoadSearchIndex() {
          const response = await fetch(`${manualRootBase}/search-index.json`);
          return response.ok ? response.json() : [];
        }
        document.addEventListener('DOMContentLoaded', () => {
          const input = document.getElementById('manual-search');
          if (!input) return;
          const results = document.createElement('div');
          results.id = 'manual-search-results';
          results.className = 'search-results';
          input.insertAdjacentElement('afterend', results);
          let searchIndex = [];
          manualLoadSearchIndex().then(index => searchIndex = index).catch(() => searchIndex = []);
          input.addEventListener('input', () => {
            const term = input.value.trim();
            const normalizedTerm = manualNormalize(term);
            document.querySelectorAll('nav a').forEach(link => {
              const text = `${link.textContent} ${link.dataset.codigoTela || ''}`;
              link.style.display = manualNormalize(text).includes(normalizedTerm) ? 'block' : 'none';
            });
            if (normalizedTerm.length < 2) {
              results.replaceChildren();
              return;
            }
            const matches = searchIndex
              .filter(item => manualNormalize(`${item.titulo} ${item.codigoTela} ${item.texto}`).includes(normalizedTerm))
              .slice(0, 12)
              .map(item => {
                const link = document.createElement('a');
                link.className = 'search-result';
                link.href = `${manualRootBase}/${item.url}`;
                const title = document.createElement('strong');
                title.textContent = item.titulo;
                const meta = document.createElement('span');
                meta.textContent = `${item.codigoTela} · ${manualSnippet(item.texto, term)}`;
                link.append(title, meta);
                return link;
              });
            results.replaceChildren(...matches);
          });
          if ('serviceWorker' in navigator && location.protocol !== 'file:') {
            navigator.serviceWorker.register(`${manualRootBase}/sw.js`).catch(() => undefined);
          }
        });
        """);
  }

  private void escreverPwa(Cliente cliente, String versao, List<Pagina> paginas, Path workDir) throws IOException {
    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("name", "Manual " + cliente.getNome());
    manifest.put("short_name", cliente.getNome());
    manifest.put("start_url", "./index.html");
    manifest.put("scope", "./");
    manifest.put("display", "standalone");
    manifest.put("background_color", cliente.getTemaCorFundoOuPadrao());
    manifest.put("theme_color", cliente.getTemaCorPrimariaOuPadrao());
    objectMapper.writeValue(workDir.resolve("manifest.webmanifest").toFile(), manifest);

    List<String> urls = new ArrayList<>();
    urls.add("./");
    urls.add("./index.html");
    urls.add("./routes.json");
    urls.add("./search-index.json");
    urls.add("./manifest.json");
    urls.add("./manifest.webmanifest");
    urls.add("./versao.html");
    String assetQ = "rev=" + MANUAL_ASSETS_REVISION + "&ver=" + URLEncoder.encode(versao, StandardCharsets.UTF_8);
    urls.add("./assets/app.css?" + assetQ);
    urls.add("./assets/app.js?" + assetQ);
    paginas.forEach(pagina -> urls.add("./paginas/" + pagina.getSlug() + ".html"));
    String cacheName = ("manual-" + cliente.getSlug() + "-" + versao).replaceAll("[^a-zA-Z0-9._-]", "-");
    Files.writeString(workDir.resolve("sw.js"), """
        const CACHE_NAME = '%s';
        const PRECACHE_URLS = %s;

        self.addEventListener('install', event => {
          event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(PRECACHE_URLS)));
          self.skipWaiting();
        });

        self.addEventListener('activate', event => {
          event.waitUntil(
            caches.keys().then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))))
          );
          self.clients.claim();
        });

        self.addEventListener('fetch', event => {
          if (event.request.method !== 'GET') return;
          event.respondWith(
            caches.match(event.request).then(cached => cached || fetch(event.request).then(response => {
              const copy = response.clone();
              caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
              return response;
            }))
          );
        });
        """.formatted(cacheName, objectMapper.writeValueAsString(urls)));
  }

  private void zipDirectory(Path sourceDir, Path zipPath) throws IOException {
    try (OutputStream fileOut = Files.newOutputStream(zipPath);
        ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
      try (var stream = Files.walk(sourceDir)) {
        for (Path path : stream.filter(Files::isRegularFile).toList()) {
          ZipEntry entry = new ZipEntry(sourceDir.relativize(path).toString().replace('\\', '/'));
          zipOut.putNextEntry(entry);
          Files.copy(path, zipOut);
          zipOut.closeEntry();
        }
      }
    }
  }

  private String sha256(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream input = Files.newInputStream(path);
          DigestInputStream digestInput = new DigestInputStream(input, digest)) {
        digestInput.transferTo(OutputStream.nullOutputStream());
      }
      byte[] hash = digest.digest();
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception ex) {
      throw new IOException("Falha ao calcular hash do pacote.", ex);
    }
  }

  public record ResultadoGeracao(
      String arquivoZipNome,
      String arquivoZipCaminho,
      String hashPacote,
      int quantidadePaginas,
      int quantidadeModulos,
      String relatorioValidacaoJson) {
  }
}
