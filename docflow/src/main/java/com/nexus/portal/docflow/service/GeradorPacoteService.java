package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.repository.ClienteModuloRepository;
import com.nexus.portal.docflow.repository.ClientePaginaRepository;
import com.nexus.portal.docflow.service.EmpresaLogoService;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaAnexo;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.shared.config.StorageProperties;
import com.nexus.portal.shared.exception.BusinessException;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class GeradorPacoteService {

  /** Revisão dos assets estáticos do manual ZIP; aparece no comentário do app.css, na meta do HTML e na query string (cache bust). */
  public static final String MANUAL_ASSETS_REVISION = "layout-v20";
  /** Grupos de sinônimos da busca ({"grupos": [["nota fiscal", "nf"]]}). */
  public static final String ARQUIVO_SINONIMOS = "sinonimos.json";

  private final ClienteModuloRepository clienteModuloRepository;
  private final ClientePaginaRepository clientePaginaRepository;
  private final com.nexus.portal.docflow.repository.ClienteProjetoRepository clienteProjetoRepository;
  private final PaginaRepository paginaRepository;
  private final PaginaAnexoRepository paginaAnexoRepository;
  private final StorageProperties storageProperties;
  private final EmpresaLogoService empresaLogoService;
  private final PaginaSnippetService paginaSnippetService;
  private final ManualSinonimoService manualSinonimoService;
  private final ManualRagService manualRagService;
  private final ObjectMapper objectMapper;

  public GeradorPacoteService(ClienteModuloRepository clienteModuloRepository,
      ClientePaginaRepository clientePaginaRepository, PaginaRepository paginaRepository,
      com.nexus.portal.docflow.repository.ClienteProjetoRepository clienteProjetoRepository,
      PaginaAnexoRepository paginaAnexoRepository,
      StorageProperties storageProperties,
      EmpresaLogoService empresaLogoService,
      PaginaSnippetService paginaSnippetService,
      ManualRagService manualRagService,
      ManualSinonimoService manualSinonimoService,
      ObjectMapper objectMapper) {
    this.clienteModuloRepository = clienteModuloRepository;
    this.clientePaginaRepository = clientePaginaRepository;
    this.clienteProjetoRepository = clienteProjetoRepository;
    this.paginaRepository = paginaRepository;
    this.paginaAnexoRepository = paginaAnexoRepository;
    this.storageProperties = storageProperties;
    this.empresaLogoService = empresaLogoService;
    this.paginaSnippetService = paginaSnippetService;
    this.manualRagService = manualRagService;
    this.manualSinonimoService = manualSinonimoService;
    this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
  }

  public ResultadoGeracao gerar(Cliente cliente, String versao) throws IOException {
    return gerar(cliente, versao, UUID.randomUUID());
  }

  public ResultadoGeracao gerar(Cliente cliente, String versao, UUID geracaoId) throws IOException {
    List<Pagina> paginas = selecionarPaginas(cliente.getId());
    if (paginas.isEmpty()) {
      throw new BusinessException("Não há páginas publicadas elegíveis para este cliente.");
    }

    String pacoteNome = "manual-%s-v%s".formatted(cliente.getSlug(), versao);
    Path storageDir = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize();
    Path workDir = storageDir.resolve("tmp").resolve(geracaoId.toString());
    Path zipPath = storageDir.resolve(pacoteNome + ".zip");

    FileSystemUtils.deleteRecursively(workDir);
    Files.createDirectories(workDir.resolve("paginas"));
    Files.createDirectories(workDir.resolve("assets"));
    Files.createDirectories(storageDir);

    escreverAssets(workDir.resolve("assets"));
    escreverLogos(cliente, workDir.resolve("assets"));
    Map<UUID, String> conteudos = escreverPaginas(cliente, versao, paginas, workDir);
    escreverJson(cliente, versao, paginas, workDir);
    // Agentes e RAG: llms.txt, llms-full.txt e um Markdown por tela em rag/ (mesmo snapshot do HTML).
    manualRagService.escrever(workDir, paginas, new ManualRagService.Escopo(
        "Manual " + cliente.getNome(), versao, cliente.getSlug(),
        pagina -> "paginas/" + pagina.getSlug() + ".html",
        pagina -> conteudos.get(pagina.getId()),
        true));
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
        "assets/app.js",
        "assets/routes.js",
        "llms.txt",
        "llms-full.txt",
        ManualRagService.PASTA + "/index.json"));
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
      String pathMd = ManualRagService.PASTA + "/" + ManualRagService.arquivo(p);
      if (!present.contains(pathMd)) {
        faltantes.add(pathMd);
      }
    }

    int htmlNoPacote = (int) present.stream()
        .filter(n -> n.startsWith("paginas/") && n.endsWith(".html"))
        .count();
    if (htmlNoPacote != paginas.size()) {
      avisos.add("Quantidade de HTML em paginas/ (%d) difere do esperado (%d)."
          .formatted(htmlNoPacote, paginas.size()));
    }

    Set<UUID> idsNoPacote = paginas.stream().map(Pagina::getId).collect(Collectors.toSet());
    for (Pagina pagina : paginas) {
      if (pagina.getParent() != null && !idsNoPacote.contains(pagina.getParent().getId())) {
        avisos.add("Subpágina sem página pai no pacote: " + pagina.getTitulo());
      }
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

    List<Pagina> publicadas = paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO);
    Map<UUID, Pagina> publicadasPorId = publicadas.stream()
        .collect(Collectors.toMap(Pagina::getId, pagina -> pagina, (a, b) -> a, LinkedHashMap::new));

    LinkedHashSet<UUID> selecionadas = publicadas.stream()
        .filter(pagina -> projetosDoCliente.contains(pagina.getModulo().getProjeto().getId())
            || modulosDoCliente.contains(pagina.getModulo().getId())
            || paginasDiretas.contains(pagina.getId()))
        .map(Pagina::getId)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    for (UUID paginaId : new ArrayList<>(selecionadas)) {
      Pagina atual = publicadasPorId.get(paginaId);
      while (atual != null && atual.getParent() != null) {
        UUID parentId = atual.getParent().getId();
        if (publicadasPorId.containsKey(parentId)) {
          selecionadas.add(parentId);
        }
        atual = publicadasPorId.get(parentId);
      }
    }

    List<Pagina> paginas = selecionadas.stream()
        .map(publicadasPorId::get)
        .filter(Objects::nonNull)
        .toList();

    return ordenarHierarquia(paginas);
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
          <section id="pagina-%d" class="page %s" data-codigo-tela="%s">
            <div class="page-client">%s</div>
            <header>
              <span>%s</span>
              <h1>%s</h1>
              %s
            </header>
            <div class="article-content">%s</div>
          </section>
          """.formatted(index + 1, index == 0 ? "active" : "", HtmlUtils.htmlEscape(pagina.getCodigoTela()),
          clientePreviewLogo,
          HtmlUtils.htmlEscape(breadcrumb(pagina)),
          HtmlUtils.htmlEscape(pagina.getTitulo()),
          pagina.getResumo() == null || pagina.getResumo().isBlank()
              ? ""
              : "<p>" + HtmlUtils.htmlEscape(pagina.getResumo()) + "</p>",
          pagina.getConteudoHtml() == null || pagina.getConteudoHtml().isBlank()
              ? "<p>Sem conteúdo HTML cadastrado.</p>"
              : paginaSnippetService.resolver(pagina.getConteudoHtml())));
    }
    return previewTemplate(cliente, versao, paginas.size(), menu, content.toString());
  }

  public String previewPagina(Pagina pagina) {
    String resumo = pagina.getResumo() == null || pagina.getResumo().isBlank()
        ? ""
        : "<p>" + HtmlUtils.htmlEscape(pagina.getResumo()) + "</p>";
    String conteudo = pagina.getConteudoHtml() == null || pagina.getConteudoHtml().isBlank()
        ? "<p>Sem conteúdo HTML cadastrado.</p>"
        : paginaSnippetService.resolver(pagina.getConteudoHtml());
    return """
        <!doctype html>
        <html lang="pt-BR">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <meta name="docflow-manual-layout" content="%s">
          <title>%s</title>
          <style>
            :root{color-scheme:light;--font-ui:Roboto,system-ui,-apple-system,"Segoe UI",Arial,sans-serif;--font-body:var(--font-ui);--border:#e5e8ee;--bg:#f7f8fa;--surface:#fff;--surface-2:#f7f8fa;--text:#0b1220;--muted:#6b7280;--accent:#4f46e5;--accent-soft:#eef0ff;--success:#10b981;--success-soft:#ecfdf5;--info:#3b82f6;--info-soft:#eff6ff;--warn:#f59e0b;--warn-soft:#fffbeb}
            *{box-sizing:border-box}body{margin:0;padding:28px;color:var(--text);background:var(--bg);font-family:var(--font-ui);font-size:14px;line-height:1.5;-webkit-font-smoothing:antialiased}
            .preview-label{display:block;max-width:1120px;margin:0 auto 12px;color:var(--muted);font-size:12px}.preview-label strong{color:var(--accent)}
            article{max-width:1120px;margin:0 auto;background:var(--surface);border:1px solid var(--border);border-radius:10px;overflow:hidden;box-shadow:0 1px 2px rgba(60,64,67,.3),0 2px 6px rgba(60,64,67,.15)}
            .article-head{padding:22px 28px 24px;border-bottom:1px solid var(--border)}.breadcrumb{color:var(--muted);font-size:12px}.article-head h1{margin:8px 0 0;font-size:26px;line-height:1.25}.article-head p{margin:10px 0 0;color:var(--muted);font-size:14px;line-height:1.65}
            .article-body{padding:28px 32px 36px;color:#3c4043;font:400 15px/1.7 var(--font-body)}.article-body>:first-child{margin-top:0}.article-body>:last-child{margin-bottom:0}
            .article-body h1,.article-body h2,.article-body h3{color:var(--text);line-height:1.3;letter-spacing:-.01em}.article-body h2{margin:1.7em 0 .65em;padding-left:14px;border-left:4px solid var(--accent);font-size:20px}.article-body h3{font-size:17px}.article-body p{margin:0 0 1em}.article-body li{margin:.4em 0}
            .article-body .doc-intro{margin:0 0 28px;padding:22px 24px;border:1px solid #c7d2fe;border-radius:12px;background:linear-gradient(135deg,var(--accent-soft),var(--surface) 78%%)}.article-body .doc-intro h2{margin:0 0 8px;padding:0;border:0}.article-body .doc-intro p{margin:0;color:var(--muted)}
            .article-body .doc-kicker{display:inline-block;margin-bottom:9px;color:var(--accent);font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}.article-body .doc-section{margin:28px 0}.article-body .doc-section--soft{padding:20px 22px;border:1px solid var(--border);border-radius:12px;background:var(--surface-2)}
            .article-body .table-wrap{margin:14px 0 24px;overflow-x:auto;border:1px solid var(--border);border-radius:12px;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body table{width:100%%;min-width:520px;border-collapse:collapse;background:var(--surface)}.article-body th,.article-body td{padding:13px 15px;border-bottom:1px solid var(--border);text-align:left}.article-body th{background:var(--accent-soft);font-size:12px;text-transform:uppercase}.article-body tr:last-child td{border-bottom:0}
            .article-body .steps>ol{display:grid;gap:12px;margin:16px 0 0;padding:0;list-style:none;counter-reset:step}.article-body .steps>ol>li{position:relative;min-height:48px;margin:0;padding:12px 16px 12px 58px;border:1px solid var(--border);border-radius:8px;counter-increment:step}.article-body .steps>ol>li:before{position:absolute;top:9px;left:12px;display:grid;width:32px;height:32px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;content:counter(step);font-weight:700}
            .article-body .checklist{display:grid;gap:9px;padding:0;list-style:none}.article-body .checklist li{position:relative;margin:0;padding:4px 0 4px 30px}.article-body .checklist li:before{position:absolute;left:0;display:grid;width:21px;height:21px;place-items:center;border-radius:50%%;background:var(--success-soft);color:var(--success);content:'✓';font-weight:700}
            .article-body .callout,.article-body .warning{position:relative;margin:22px 0;padding:16px 18px 16px 54px;border:1px solid #a5b4fc;border-radius:12px;background:var(--info-soft)}.article-body .warning{border-color:#f9d58c;background:var(--warn-soft)}.article-body .callout:before,.article-body .warning:before{position:absolute;top:15px;left:16px;display:grid;width:24px;height:24px;place-items:center;border-radius:50%%;background:var(--info);color:#fff;content:'i';font-weight:700}.article-body .warning:before{background:var(--warn);content:'!'}
            .article-body .faq-list{display:grid;gap:12px}.article-body .faq-item{padding:17px 19px;border:1px solid var(--border);border-radius:12px;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body .faq-item h3{margin:0 0 7px;color:var(--accent)}.article-body .faq-item p:last-child{margin:0;color:var(--muted)}
            .article-body .result-card{margin:22px 0;padding:18px 20px;border:1px solid #6ee7b7;border-radius:12px;background:var(--success-soft)}.article-body .result-card strong{display:block;margin-bottom:5px;color:var(--success)}
            .article-body .objective-card{position:relative;margin:24px 0 30px;padding:18px 22px 18px 62px;border:2px solid var(--accent);border-radius:12px;background:#fff;box-shadow:0 8px 24px rgba(79,70,229,.09)}.article-body .objective-card:before{position:absolute;top:17px;left:20px;display:grid;width:28px;height:28px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;content:'i';font-weight:800}.article-body .objective-card strong{display:block;color:var(--accent)}.article-body .objective-card p{margin:4px 0 0;color:var(--muted)}
            .article-body .screen-frame{margin:15px 0 26px;padding:12px;border:1px solid #c7d2fe;border-radius:12px;background:var(--surface-2)}.article-body .screen-frame img{width:100%%;margin:0}.article-body .screen-frame figcaption{padding:10px 5px 2px;color:var(--muted);font-size:12px;text-align:center}.article-body .screen-placeholder{display:grid;min-height:260px;padding:30px;place-content:center;border:2px dashed #a5b4fc;border-radius:9px;background:#fff;color:var(--muted);text-align:center}.article-body .screen-placeholder strong{display:block;color:var(--accent)}.article-body .screen-placeholder span{font-size:12px}.article-body .screen-placeholder--compact{min-height:150px;padding:20px;border-width:1px}
            .article-body .content-grid,.article-body .annotation-grid,.article-body .screen-grid{display:grid;gap:16px}.article-body .content-grid--2{grid-template-columns:repeat(2,minmax(0,1fr))}.article-body .content-grid--3,.article-body .annotation-grid,.article-body .screen-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.article-body .annotation-grid{counter-reset:annotation}.article-body .annotation-card,.article-body .rule-card,.article-body .guide-card,.article-body .topic-card{position:relative;padding:18px;border:1px solid var(--border);border-radius:12px;background:#fff;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body .annotation-card{padding-left:58px;counter-increment:annotation}.article-body .annotation-card:before{position:absolute;top:17px;left:17px;display:grid;width:28px;height:28px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;content:counter(annotation);font-size:12px;font-weight:800}.article-body .rule-card{border-top:3px solid var(--accent)}.article-body .annotation-card h3,.article-body .rule-card h2,.article-body .rule-card h3,.article-body .guide-card h3,.article-body .topic-card h3{margin:0 0 7px;padding:0;border:0}.article-body .guide-card{padding:12px 12px 20px}.article-body .guide-card__number{position:absolute;z-index:1;top:22px;left:22px;display:grid;width:32px;height:32px;place-items:center;border:3px solid #fff;border-radius:50%%;background:var(--accent);color:#fff;font-weight:800}.article-body .topic-card{text-align:center}.article-body .topic-card__icon{display:grid;width:46px;height:46px;margin:0 auto 14px;place-items:center;border-radius:12px;background:var(--accent-soft);color:var(--accent);font-weight:800}
            .article-body .flow-strip{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;margin:18px 0 28px;padding:0;list-style:none;counter-reset:flow}.article-body .flow-strip li{position:relative;min-height:118px;margin:0;padding:54px 16px 16px;border:1px solid var(--border);border-radius:12px;background:#fff;text-align:center;counter-increment:flow}.article-body .flow-strip li:before{position:absolute;top:14px;left:50%%;display:grid;width:30px;height:30px;place-items:center;transform:translateX(-50%%);border-radius:50%%;background:var(--accent);color:#fff;content:counter(flow);font-weight:800}.article-body .flow-strip li:not(:last-child):after{position:absolute;top:48px;right:-19px;color:var(--accent);content:'→';font-size:20px;font-weight:800}.article-body .flow-strip strong,.article-body .flow-strip span{display:block}.article-body .flow-strip span{color:var(--muted);font-size:12px}.article-body .number-badge{display:grid;width:26px;height:26px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;font-size:11px;font-weight:800}.article-body .status-badge{display:inline-flex;padding:3px 9px;border-radius:999px;background:var(--surface-2);color:var(--muted);font-size:11px;font-weight:800}.article-body .status-badge--required,.article-body .status-badge--sim{background:var(--accent-soft);color:var(--accent)}.article-body .status-badge--ok,.article-body .status-badge--ativo{background:var(--success-soft);color:var(--success)}.article-body .status-badge--nao,.article-body .status-badge--off,.article-body .status-badge--inativo{background:#f3f4f6;color:#6b7280}.article-body .status-badge--warn{background:var(--warn-soft);color:var(--warn)}.article-body .status-badge--danger{background:#fef2f2;color:#ef4444}.article-body .callout--danger{border-color:#fecaca;background:#fef2f2}.article-body .callout--danger:before{background:#ef4444;content:'!'}.article-body .condition-stack{display:grid;grid-template-columns:1fr 1fr;gap:16px}.article-body .condition-block{padding:18px 20px;border:1px solid var(--border);border-radius:12px;background:#fff;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body .condition-block__label{display:inline-flex;margin-bottom:10px;padding:3px 10px;border-radius:999px;background:var(--surface-2);color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.08em}.article-body .condition-block--then .condition-block__label{background:var(--accent);color:#fff}.article-body .condition-block h3{margin:0 0 8px}.article-body .condition-block p{margin:0;color:var(--muted)}.article-body .decision-board{display:grid;grid-template-columns:1fr 1.2fr;gap:16px}.article-body .annotation-grid--1,.article-body .content-grid--1{grid-template-columns:minmax(0,1fr)}.article-body .annotation-grid--half{grid-template-columns:minmax(0,.5fr)}.article-body .annotation-grid--3{grid-template-columns:repeat(3,minmax(0,1fr))}
            .article-body .doc-intro--center{text-align:center}.article-body .filter-chips p{display:flex;flex-wrap:wrap;gap:9px;margin:0}.article-body .filter-chip{display:inline-flex;padding:7px 16px;border:1px solid var(--border);border-radius:999px;color:var(--muted);font-size:12px;font-weight:700}.article-body .filter-chip--active{border-color:var(--accent);background:var(--accent);color:#fff}.article-body .resource-list{display:grid;overflow:hidden;border:1px solid var(--border);border-radius:12px;background:#fff}.article-body .resource-item{display:flex;min-width:0;padding:12px 15px;align-items:center;gap:12px;border-bottom:1px solid var(--border)}.article-body .resource-item:last-child{border-bottom:0}.article-body .resource-item>span:not(.number-badge){display:grid;min-width:0;flex:1}.article-body .resource-item strong{color:var(--accent)}.article-body .resource-item small,.article-body .resource-item>span:last-child{color:var(--muted);font-size:11px}.article-body .resource-item__meta{flex:0 0 auto!important;text-align:right}.article-body .rank-list{display:grid;gap:9px;padding:0;list-style:none;counter-reset:rank}.article-body .rank-list li{position:relative;margin:0;padding-left:34px;counter-increment:rank}.article-body .rank-list li:before{position:absolute;left:0;display:grid;width:24px;height:24px;place-items:center;border:1px solid #a5b4fc;border-radius:50%%;color:var(--accent);content:counter(rank);font-size:11px;font-weight:800}
            .article-body .journey-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:16px}.article-body .journey-card{position:relative;min-height:178px;padding:54px 17px 18px;border:1px solid var(--border);border-radius:12px;background:#fff;text-align:center}.article-body .journey-card:not(:last-child):after{position:absolute;top:50%%;right:-15px;color:var(--accent);content:'→';font-weight:800}.article-body .journey-card__number{position:absolute;top:15px;left:15px;display:grid;width:27px;height:27px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;font-size:11px;font-weight:800}.article-body .journey-card h3{margin:0 0 8px;color:var(--accent)}.article-body .journey-card p{margin:0;color:var(--muted);font-size:12px}.article-body .status-list{display:grid;padding:0;overflow:hidden;border:1px solid var(--border);border-radius:8px;list-style:none}.article-body .status-item{display:flex;margin:0;padding:10px 13px;justify-content:space-between;gap:12px;border-bottom:1px solid var(--border)}.article-body .status-item:last-child{border-bottom:0}.article-body .status-item strong{color:var(--muted);font-size:11px}.article-body .status-item--done strong{color:var(--success)}.article-body .status-item--progress strong{color:var(--warn)}.article-body .related-links{margin-top:26px;padding-top:18px;border-top:1px solid var(--border)}.article-body .related-links p{display:flex;flex-wrap:wrap;gap:10px 24px;margin:0}.article-body .related-links strong{flex-basis:100%%}.article-body .related-links span{color:var(--accent);font-size:12px;font-weight:700}.article-body .related-links span:after{margin-left:7px;content:'→'}.article-body [data-codigo-tela],.article-content [data-codigo-tela]{cursor:pointer;color:var(--accent);font-weight:700}.article-body [data-codigo-tela]:hover,.article-content [data-codigo-tela]:hover{text-decoration:underline}.article-body .related-links [data-codigo-tela],.article-content .related-links [data-codigo-tela]{display:inline}
            .article-body img{display:block;max-width:100%%;height:auto;margin:18px 0;border:1px solid var(--border);border-radius:10px}.article-body pre{overflow:auto;padding:16px 18px;border-radius:8px;background:#202124;color:#e8eaed}
            @media(max-width:700px){body{padding:12px}.article-body{padding:22px}.article-head{padding:20px}.article-body .doc-intro,.article-body .doc-section--soft{padding:17px}.article-body .content-grid--2,.article-body .content-grid--3,.article-body .annotation-grid,.article-body .screen-grid,.article-body .flow-strip,.article-body .journey-grid{grid-template-columns:1fr}.article-body .flow-strip li:not(:last-child):after,.article-body .journey-card:not(:last-child):after{top:auto;right:50%%;bottom:-25px;transform:translateX(50%%) rotate(90deg)}.article-body .resource-item{align-items:flex-start;flex-wrap:wrap}.article-body .resource-item__meta{flex-basis:100%%!important;text-align:left}}
          </style>
        </head>
        <body>
          <span class="preview-label"><strong>Prévia fiel</strong> · renderizador %s</span>
          <article>
            <header class="article-head"><span class="breadcrumb">%s</span><h1>%s</h1>%s</header>
            <div class="article-body">%s</div>
          </article>
        </body>
        </html>
        """.formatted(MANUAL_ASSETS_REVISION, HtmlUtils.htmlEscape(pagina.getTitulo()),
        MANUAL_ASSETS_REVISION, HtmlUtils.htmlEscape(breadcrumb(pagina)),
        HtmlUtils.htmlEscape(pagina.getTitulo()), resumo, conteudo);
  }

  /** Devolve o conteúdo final de cada página (trechos resolvidos, anexos copiados) para reuso. */
  private Map<UUID, String> escreverPaginas(Cliente cliente, String versao, List<Pagina> paginas, Path workDir)
      throws IOException {
    Map<UUID, String> conteudos = new LinkedHashMap<>();
    for (Pagina pagina : paginas) {
      String menu = menuHtml(paginas, "", pagina.getSlug());
      String conteudo = conteudoDoMenu(pagina,
          prepararConteudoComAnexos(pagina, workDir.resolve("assets"), "../assets"), paginas);
      String html = template(cliente, versao, pagina.getTitulo(), breadcrumb(pagina), menu,
          conteudo, "../assets");
      // INT-605: a página se identifica para o registro de "página aberta".
      html = html.replaceFirst("<head>", "<head>\n  <meta name=\"docflow-codigo-tela\" content=\""
          + HtmlUtils.htmlEscape(pagina.getCodigoTela()) + "\">");
      Files.writeString(workDir.resolve("paginas").resolve(pagina.getSlug() + ".html"), html);
      conteudos.put(pagina.getId(), conteudo);
    }
    return conteudos;
  }

  /**
   * PLAT-02: um menu sem a lista das subpáginas ganha "Nesta seção" gerada. Os itens levam
   * {@code data-codigo-tela}: viram link para o .md da tela na base de RAG.
   */
  static String conteudoDoMenu(Pagina pagina, String conteudo, List<Pagina> paginas) {
    if (!pagina.menu()) {
      return conteudo;
    }
    List<Pagina> filhos = paginas.stream()
        .filter(p -> p.getParent() != null && p.getParent().getId().equals(pagina.getId()))
        .toList();
    org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(conteudo == null ? "" : conteudo);
    boolean jaLista = !filhos.isEmpty() && filhos.stream().allMatch(f ->
        !doc.select("a[href*=\"" + f.getSlug() + ".html\"], [data-codigo-tela=\""
            + f.getCodigoTela().replace("\"", "") + "\"]").isEmpty());
    if (filhos.isEmpty() || jaLista) {
      return conteudo;
    }
    StringBuilder lista = new StringBuilder("<section class=\"doc-section menu-filhos\"><h2>Nesta seção</h2><ul>");
    for (Pagina filho : filhos) {
      lista.append("<li><a href=\"").append(HtmlUtils.htmlEscape(filho.getSlug())).append(".html\" data-codigo-tela=\"")
          .append(HtmlUtils.htmlEscape(filho.getCodigoTela())).append("\">")
          .append(HtmlUtils.htmlEscape(filho.getTitulo())).append("</a>");
      if (filho.getResumo() != null && !filho.getResumo().isBlank()) {
        lista.append(" — ").append(HtmlUtils.htmlEscape(filho.getResumo().strip()));
      }
      lista.append("</li>");
    }
    return (conteudo == null ? "" : conteudo) + lista.append("</ul></section>");
  }

  private String prepararConteudoComAnexos(Pagina pagina, Path assetsDir, String assetBase) throws IOException {
    // Os trechos reutilizáveis viram HTML aqui: o pacote é estático e não tem
    // como resolver {{snippet:...}} depois de gerado.
    String conteudo = paginaSnippetService.resolver(
        pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml());
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
    // Ponte para os sistemas do cliente abrirem a ajuda da tela (Onda D); com o ZIP local,
    // NexusManual.configure({ localUrl }) resolve sem a API.
    try (var helpBridge = GeradorPacoteService.class.getResourceAsStream("/docflow/help-bridge.js")) {
      if (helpBridge != null) {
        Files.write(workDir.resolve("assets").resolve("help-bridge.js"), helpBridge.readAllBytes());
      }
    }
    // Aberto do disco (file://) o navegador bloqueia fetch: o deep link lê as rotas deste script.
    Files.writeString(workDir.resolve("assets").resolve("routes.js"),
        "window.MANUAL_ROUTES = "
            + objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(routes) + ";\n");
    objectMapper.writeValue(workDir.resolve("search-index.json").toFile(), searchIndex);
    // Cópia dos sinônimos para a busca offline; hospedado pela API, o arquivo vem do banco (atual).
    objectMapper.writeValue(workDir.resolve(ARQUIVO_SINONIMOS).toFile(),
        Map.of("grupos", manualSinonimoService.grupos(cliente.getId())));
    objectMapper.writeValue(workDir.resolve("manifest.json").toFile(), manifest);
  }

  private void escreverIndex(Cliente cliente, String versao, List<Pagina> paginas, Path workDir) throws IOException {
    String cards = paginas.stream()
        .map(p -> """
            <a class="welcome-page-card" href="paginas/%s.html" data-welcome-page data-search="%s %s %s %s">
              <span class="welcome-page-card__module">%s</span>
              <strong>%s</strong>
              <small>%s</small>
              <span class="welcome-page-card__action">Abrir guia <b>→</b></span>
            </a>
            """.formatted(p.getSlug(), HtmlUtils.htmlEscape(p.getModulo().getNome()),
            HtmlUtils.htmlEscape(p.getTitulo()), HtmlUtils.htmlEscape(p.getCodigoTela()),
            HtmlUtils.htmlEscape(p.getResumo() == null ? "" : p.getResumo()),
            HtmlUtils.htmlEscape(p.getModulo().getNome()), HtmlUtils.htmlEscape(p.getTitulo()),
            HtmlUtils.htmlEscape(p.getResumo() == null || p.getResumo().isBlank()
                ? p.getCodigoTela() : p.getResumo())))
        .reduce("", String::concat);
    String primeiroGuia = paginas.isEmpty() ? "versao.html" : "paginas/" + paginas.getFirst().getSlug() + ".html";
    String tituloPrimeiroGuia = paginas.isEmpty() ? "Conheça esta publicação" : paginas.getFirst().getTitulo();
    long quantidadeModulos = paginas.stream().map(pagina -> pagina.getModulo().getId()).distinct().count();
    String content = """
        <section class="welcome-hero">
          <div class="welcome-hero__glow"></div>
          <span class="welcome-eyebrow">CENTRAL DE AJUDA</span>
          <h1>Olá! Como podemos ajudar?</h1>
          <p>Bem-vindo ao help de <strong>%s</strong>. Encontre orientações claras para realizar cada tarefa com segurança e confiança.</p>
          <label class="welcome-search" for="welcome-search">
            <span aria-hidden="true">⌕</span>
            <input id="welcome-search" type="search" autocomplete="off" placeholder="Digite uma tela, assunto ou código para começar">
          </label>
          <p id="welcome-search-status" class="welcome-search-status" aria-live="polite">Explore os guias disponíveis abaixo.</p>
          <div class="welcome-hero__actions">
            <a class="welcome-button welcome-button--primary" href="%s">Começar por aqui <span>→</span></a>
            <a class="welcome-button" href="#guias">Ver todos os guias</a>
          </div>
        </section>
        <section class="welcome-overview" aria-label="Resumo do manual">
          <div><strong>%d</strong><span>guias práticos</span></div>
          <div><strong>%d</strong><span>módulos cobertos</span></div>
          <a href="versao.html"><span>Esta publicação</span><strong>v%s</strong></a>
        </section>
        <section class="welcome-paths" aria-label="Caminhos rápidos">
          <a href="%s" class="welcome-path">
            <span class="welcome-path__icon">1</span><span><strong>Comece um procedimento</strong><small>Acesse %s</small></span><b>→</b>
          </a>
          <a href="#guias" class="welcome-path">
            <span class="welcome-path__icon">⌘</span><span><strong>Encontre uma tela</strong><small>Pesquise ou navegue pelos guias</small></span><b>→</b>
          </a>
          <a href="versao.html" class="welcome-path">
            <span class="welcome-path__icon">i</span><span><strong>Consulte a versão</strong><small>Veja o escopo deste manual</small></span><b>→</b>
          </a>
        </section>
        <section id="guias" class="welcome-guides">
          <div class="welcome-section-heading">
            <div><span>GUIAS DISPONÍVEIS</span><h2>Encontre a resposta certa</h2></div>
            <p>Selecione um guia para ver o passo a passo.</p>
          </div>
          <div id="welcome-pages" class="welcome-pages">%s</div>
          <div id="welcome-empty" class="welcome-empty" hidden>
            <strong>Nenhum guia encontrado.</strong><span>Tente outro termo ou navegue pelo menu lateral.</span>
          </div>
        </section>
        """.formatted(HtmlUtils.htmlEscape(cliente.getNome()), primeiroGuia,
        paginas.size(), quantidadeModulos, HtmlUtils.htmlEscape(versao), primeiroGuia,
        HtmlUtils.htmlEscape(tituloPrimeiroGuia), cards);
    Files.writeString(workDir.resolve("index.html"), template(cliente, versao,
        "Manual " + cliente.getNome(), "Início", menuHtml(paginas, "paginas/", null),
        content, "assets"));
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
        "Publicação / Sobre esta versão", menuHtml(paginas, "paginas/", null), content, "assets"));
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
            .shell{display:grid;grid-template-columns:260px minmax(0,1fr);grid-template-rows:1fr;min-height:100vh;width:100%%;align-items:stretch}
            aside{background:var(--surface);color:var(--text);border-right:1px solid var(--border);padding:20px 12px;position:sticky;top:0;height:100vh;overflow:auto;box-shadow:1px 0 0 rgba(0,0,0,.06)}
            .brand-panel{margin:-20px -12px 16px -12px;padding:16px 12px 14px;background:var(--surface);border-bottom:1px solid var(--border)}
            .brand{display:flex;align-items:center;justify-content:center;padding:4px 0}
            .brand-logo-empresa{width:100%%;max-height:56px;object-fit:contain}
            .brand-text{font-weight:700;font-size:18px;color:var(--accent);justify-content:center;letter-spacing:-0.01em}
            .preview-versao{color:var(--muted);font-size:12px;line-height:16px;margin:10px 0 0;text-align:center;font-weight:400}
            aside nav{margin-top:4px}
            aside ul{list-style:none;margin:0;padding:0}aside li{margin:2px 0}
            aside a{display:grid;gap:2px;color:#3c4043;text-decoration:none;padding:10px 16px;border-radius:999px;font-size:14px;font-weight:400;line-height:20px;transition:background .15s,color .15s}
            aside a:hover{background:#f1f3f4;color:#202124}
            aside a.active{background:var(--accent-soft);color:var(--accent-hover);font-weight:500}
            aside a span{color:var(--muted);font-size:12px;line-height:16px;font-weight:400}
            main{padding:24px clamp(16px,2.5vw,28px) 32px;width:100%%;min-width:0;max-width:none;margin:0;display:flex;flex-direction:column;align-self:stretch}
            .page-client{display:flex;justify-content:flex-end;align-items:center;padding:14px 28px;background:#fafafa;border-bottom:1px solid var(--border)}
            .cliente-logo{height:48px;width:auto;max-width:220px;object-fit:contain}
            .cliente-nome{font-size:14px;font-weight:500;color:#3c4043;white-space:nowrap;font-family:var(--font-ui)}
            .page{display:none;width:100%%;max-width:none;margin:0;background:var(--surface);border:1px solid var(--border);border-radius:8px;overflow:hidden;box-shadow:0 1px 2px 0 rgba(60,64,67,.3),0 2px 6px 2px rgba(60,64,67,.15);flex:1 1 auto;min-width:0}
            .page.active{display:block}.page header{border-bottom:1px solid var(--border);padding:22px 28px 26px}
            .page header span{color:var(--muted);font-size:12px;letter-spacing:.1px;font-family:var(--font-ui)}.page h1{margin:8px 0 0;font-size:clamp(1.35rem,2.1vw,1.625rem);font-weight:600;line-height:1.25;font-family:var(--font-body);color:var(--text)}
            .page p{line-height:1.65;font-family:var(--font-body)}.article-content{padding:28px 28px 34px;line-height:1.65;font-family:var(--font-body);font-size:15px;color:#3c4043}.article-content :first-child{margin-top:0}.article-content :last-child{margin-bottom:0}
            .article-content a{color:var(--accent)}
            .article-content img{max-width:100%%;height:auto;display:block;border-radius:8px;margin:12px 0}
            .article-content figure.photo{margin:12px 0}.article-content figure.photo figcaption{font-size:12px;color:var(--muted);margin-top:6px;font-family:var(--font-ui)}
            .article-content h2{padding-left:14px;border-left:4px solid var(--accent);font-size:1.25rem}.article-content h3{font-size:1.0625rem}.article-content h2,.article-content h3{color:var(--text);line-height:1.3;margin:1.35em 0 .55em}
            .article-content .doc-intro{margin:0 0 28px;padding:22px 24px;border:1px solid #c7d2fe;border-radius:12px;background:linear-gradient(135deg,var(--accent-soft),var(--surface) 78%%)}.article-content .doc-intro h2{margin:0 0 8px;padding:0;border:0}.article-content .doc-intro p{margin:0;color:var(--muted)}.article-content .doc-kicker{display:inline-block;margin-bottom:9px;color:var(--accent);font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}
            .article-content .doc-section{margin:28px 0}.article-content .doc-section--soft{padding:20px 22px;border:1px solid var(--border);border-radius:12px;background:#f7f8fa}.article-content .table-wrap{margin:14px 0 24px;overflow-x:auto;border:1px solid var(--border);border-radius:12px}.article-content table{width:100%%;min-width:520px;border-collapse:collapse}.article-content th,.article-content td{padding:13px 15px;border-bottom:1px solid var(--border);text-align:left}.article-content th{background:var(--accent-soft);font-size:12px;text-transform:uppercase}
            .article-content .steps>ol{display:grid;gap:12px;margin:16px 0 0;padding:0;list-style:none;counter-reset:step}.article-content .steps>ol>li{position:relative;min-height:48px;margin:0;padding:12px 16px 12px 58px;border:1px solid var(--border);border-radius:8px;counter-increment:step}.article-content .steps>ol>li:before{position:absolute;top:9px;left:12px;display:grid;width:32px;height:32px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;content:counter(step);font-weight:700}
            .article-content .checklist{display:grid;gap:9px;padding:0;list-style:none}.article-content .checklist li{position:relative;margin:0;padding:4px 0 4px 30px}.article-content .checklist li:before{position:absolute;left:0;content:'✓';color:#10b981;font-weight:700}.article-content .callout,.article-content .warning{position:relative;margin:22px 0;padding:16px 18px 16px 54px;border:1px solid #a5b4fc;border-radius:12px;background:#eff6ff}.article-content .warning{border-color:#fcd34d;background:#fffbeb}
            .article-content .faq-list{display:grid;gap:12px}.article-content .faq-item{padding:17px 19px;border:1px solid var(--border);border-radius:12px}.article-content .faq-item h3{margin:0 0 7px;color:var(--accent)}.article-content .result-card{margin:22px 0;padding:18px 20px;border:1px solid #a8dab5;border-radius:12px;background:#ecfdf5}.article-content .result-card strong{display:block;margin-bottom:5px;color:#10b981}
            .article-content .objective-card{position:relative;margin:24px 0 30px;padding:18px 22px 18px 62px;border:2px solid var(--accent);border-radius:12px;background:#fff}.article-content .objective-card:before{position:absolute;top:17px;left:20px;display:grid;width:28px;height:28px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;content:'i';font-weight:700}.article-content .objective-card strong{display:block;color:var(--accent)}.article-content .objective-card p{margin:4px 0 0;color:var(--muted)}
            .article-content .screen-frame{margin:15px 0 26px;padding:12px;border:1px solid #c7d2fe;border-radius:12px;background:#f7f8fa}.article-content .screen-frame img{width:100%%;margin:0}.article-content .screen-frame figcaption{padding:10px 5px 2px;color:var(--muted);font-size:12px;text-align:center}.article-content .screen-placeholder{display:grid;min-height:260px;padding:30px;place-content:center;border:2px dashed #a5b4fc;border-radius:9px;background:#fff;color:var(--muted);text-align:center}.article-content .screen-placeholder strong{display:block;color:var(--accent)}.article-content .screen-placeholder span{font-size:12px}.article-content .screen-placeholder--compact{min-height:150px;padding:20px;border-width:1px}
            .article-content .content-grid,.article-content .annotation-grid,.article-content .screen-grid{display:grid;gap:16px}.article-content .content-grid--2{grid-template-columns:repeat(2,minmax(0,1fr))}.article-content .content-grid--3,.article-content .annotation-grid,.article-content .screen-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.article-content .annotation-grid{counter-reset:annotation}.article-content .annotation-card,.article-content .rule-card,.article-content .guide-card,.article-content .topic-card{position:relative;padding:18px;border:1px solid var(--border);border-radius:12px;background:#fff}.article-content .annotation-card{padding-left:58px;counter-increment:annotation}.article-content .annotation-card:before{position:absolute;top:17px;left:17px;display:grid;width:28px;height:28px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;content:counter(annotation);font-size:12px;font-weight:700}.article-content .rule-card{border-top:3px solid var(--accent)}.article-content .annotation-card h3,.article-content .rule-card h2,.article-content .rule-card h3,.article-content .guide-card h3,.article-content .topic-card h3{margin:0 0 7px;padding:0;border:0}.article-content .guide-card{padding:12px 12px 20px}.article-content .guide-card__number{position:absolute;z-index:1;top:22px;left:22px;display:grid;width:32px;height:32px;place-items:center;border:3px solid #fff;border-radius:50%%;background:var(--accent);color:#fff;font-weight:700}.article-content .topic-card{text-align:center}.article-content .topic-card__icon{display:grid;width:46px;height:46px;margin:0 auto 14px;place-items:center;border-radius:12px;background:var(--accent-soft);color:var(--accent);font-weight:700}
            .article-content .flow-strip{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;margin:18px 0 28px;padding:0;list-style:none;counter-reset:flow}.article-content .flow-strip li{position:relative;min-height:118px;margin:0;padding:54px 16px 16px;border:1px solid var(--border);border-radius:12px;background:#fff;text-align:center;counter-increment:flow}.article-content .flow-strip li:before{position:absolute;top:14px;left:50%%;display:grid;width:30px;height:30px;place-items:center;transform:translateX(-50%%);border-radius:50%%;background:var(--accent);color:#fff;content:counter(flow);font-weight:700}.article-content .flow-strip li:not(:last-child):after{position:absolute;top:48px;right:-19px;color:var(--accent);content:'→';font-size:20px;font-weight:700}.article-content .flow-strip strong,.article-content .flow-strip span{display:block}.article-content .flow-strip span{color:var(--muted);font-size:12px}.article-content .number-badge{display:grid;width:26px;height:26px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;font-size:11px;font-weight:700}.article-content .status-badge{display:inline-flex;padding:3px 9px;border-radius:999px;background:#f7f8fa;color:var(--muted);font-size:11px;font-weight:700}.article-content .status-badge--required,.article-content .status-badge--sim{background:var(--accent-soft);color:var(--accent)}.article-content .status-badge--ok,.article-content .status-badge--ativo{background:var(--success-soft);color:var(--success)}.article-content .status-badge--nao,.article-content .status-badge--off,.article-content .status-badge--inativo{background:#f3f4f6;color:#6b7280}.article-content .status-badge--warn{background:var(--warn-soft);color:var(--warn)}.article-content .status-badge--danger{background:#fef2f2;color:#ef4444}.article-content .callout--danger{border-color:#fecaca;background:#fef2f2}.article-content .callout--danger:before{background:#ef4444;content:'!'}.article-content .condition-stack{display:grid;grid-template-columns:1fr 1fr;gap:16px}.article-content .condition-block{padding:18px 20px;border:1px solid var(--border);border-radius:12px;background:#fff;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-content .condition-block__label{display:inline-flex;margin-bottom:10px;padding:3px 10px;border-radius:999px;background:var(--surface-2);color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.08em}.article-content .condition-block--then .condition-block__label{background:var(--accent);color:#fff}.article-content .condition-block h3{margin:0 0 8px}.article-content .condition-block p{margin:0;color:var(--muted)}.article-content .decision-board{display:grid;grid-template-columns:1fr 1.2fr;gap:16px}.article-content .annotation-grid--1,.article-content .content-grid--1{grid-template-columns:minmax(0,1fr)}.article-content .annotation-grid--half{grid-template-columns:minmax(0,.5fr)}.article-content .annotation-grid--3{grid-template-columns:repeat(3,minmax(0,1fr))}
            .article-content .doc-intro--center{text-align:center}.article-content .filter-chips p{display:flex;flex-wrap:wrap;gap:9px;margin:0}.article-content .filter-chip{display:inline-flex;padding:7px 16px;border:1px solid var(--border);border-radius:999px;color:var(--muted);font-size:12px;font-weight:700}.article-content .filter-chip--active{border-color:var(--accent);background:var(--accent);color:#fff}.article-content .resource-list{display:grid;overflow:hidden;border:1px solid var(--border);border-radius:12px;background:#fff}.article-content .resource-item{display:flex;padding:12px 15px;align-items:center;gap:12px;border-bottom:1px solid var(--border)}.article-content .resource-item:last-child{border-bottom:0}.article-content .resource-item>span:not(.number-badge){display:grid;min-width:0;flex:1}.article-content .resource-item strong{color:var(--accent)}.article-content .resource-item small,.article-content .resource-item>span:last-child{color:var(--muted);font-size:11px}.article-content .resource-item__meta{flex:0 0 auto!important;text-align:right}.article-content .rank-list{display:grid;gap:9px;padding:0;list-style:none;counter-reset:rank}.article-content .rank-list li{position:relative;margin:0;padding-left:34px;counter-increment:rank}.article-content .rank-list li:before{position:absolute;left:0;display:grid;width:24px;height:24px;place-items:center;border:1px solid #a5b4fc;border-radius:50%%;color:var(--accent);content:counter(rank);font-size:11px;font-weight:700}
            .article-content .journey-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:16px}.article-content .journey-card{position:relative;min-height:178px;padding:54px 17px 18px;border:1px solid var(--border);border-radius:12px;background:#fff;text-align:center}.article-content .journey-card:not(:last-child):after{position:absolute;top:50%%;right:-15px;color:var(--accent);content:'→';font-weight:700}.article-content .journey-card__number{position:absolute;top:15px;left:15px;display:grid;width:27px;height:27px;place-items:center;border-radius:50%%;background:var(--accent);color:#fff;font-size:11px;font-weight:700}.article-content .journey-card h3{margin:0 0 8px;color:var(--accent)}.article-content .journey-card p{margin:0;color:var(--muted);font-size:12px}.article-content .status-list{display:grid;padding:0;overflow:hidden;border:1px solid var(--border);border-radius:8px;list-style:none}.article-content .status-item{display:flex;margin:0;padding:10px 13px;justify-content:space-between;gap:12px;border-bottom:1px solid var(--border)}.article-content .status-item:last-child{border-bottom:0}.article-content .status-item strong{color:var(--muted);font-size:11px}.article-content .status-item--done strong{color:#10b981}.article-content .status-item--progress strong{color:#f59e0b}.article-content .related-links{margin-top:26px;padding-top:18px;border-top:1px solid var(--border)}.article-content .related-links p{display:flex;flex-wrap:wrap;gap:10px 24px;margin:0}.article-content .related-links strong{flex-basis:100%%}.article-content .related-links span{color:var(--accent);font-size:12px;font-weight:700}.article-content .related-links span:after{margin-left:7px;content:'→'}
            @media(max-width:900px){.shell{display:block;grid-template-columns:1fr}aside{position:static;height:auto}main{padding:16px}.article-content .content-grid--2,.article-content .content-grid--3,.article-content .annotation-grid,.article-content .screen-grid,.article-content .flow-strip,.article-content .journey-grid{grid-template-columns:1fr}.article-content .flow-strip li:not(:last-child):after,.article-content .journey-card:not(:last-child):after{top:auto;right:50%%;bottom:-25px;transform:translateX(50%%) rotate(90deg)}.article-content .resource-item{align-items:flex-start;flex-wrap:wrap}.article-content .resource-item__meta{flex-basis:100%%!important;text-align:left}}
            .ask{margin:0 0 20px;padding:16px 18px;border:1px solid var(--border);border-radius:12px;background:var(--surface)}.ask[hidden]{display:none}.ask form{display:flex;gap:8px}.ask input{flex:1;min-width:0;padding:10px 12px;border:1px solid var(--border);border-radius:8px;font:inherit}.ask button{padding:10px 16px;border:0;border-radius:8px;background:var(--accent);color:#fff;font-weight:600;cursor:pointer}.ask button:disabled{opacity:.6;cursor:wait}.ask-label{display:block;margin-bottom:8px;font-weight:600}.ask-resposta{margin-top:12px;white-space:pre-wrap;line-height:1.55}.ask-resposta--nao-sei{color:var(--muted)}.ask-citacoes{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px;padding:0;list-style:none}.ask-citacoes button{padding:5px 10px;border:1px solid var(--accent);border-radius:999px;background:var(--accent-soft);color:var(--accent);font-size:12px;font-weight:600}.ask-aviso{margin-top:8px;color:var(--muted);font-size:12px}
          </style>
        </head>
        <body>
          <div class="shell">
            <aside>
              %s
              <nav>%s</nav>
            </aside>
            <main>
              <section class="ask" id="ask" hidden aria-label="Pergunte ao manual">
                <label class="ask-label" for="ask-pergunta">Pergunte ao manual</label>
                <form id="ask-form">
                  <input id="ask-pergunta" maxlength="500" placeholder="Ex.: como filtrar os pedidos por status?" autocomplete="off">
                  <button type="submit">Perguntar</button>
                </form>
                <div id="ask-saida" aria-live="polite"></div>
              </section>
              %s
            </main>
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
            // Deep link por código de tela: ?tela=PED-001 ou #tela=PED-001 (INT-105).
            const telaPedida=new URLSearchParams(location.search).get('tela')||(location.hash.startsWith('#tela=')?decodeURIComponent(location.hash.slice(6)):null);
            const paginaDaTela=telaPedida?pages.find(page=>page.dataset.codigoTela===telaPedida):null;
            const initial=paginaDaTela?paginaDaTela.id:(location.hash&&!telaPedida?location.hash.slice(1):links[0]?.dataset.page);
            if(initial)openPage(initial,!location.hash||!!paginaDaTela);
            // Pergunte ao manual (INT-503): só na prévia por token; responde com o manual vigente do cliente.
            (function(){
              const m=location.pathname.match(/^(.*)\\/preview\\/([^/?#]+)/);
              const ask=document.getElementById('ask');
              if(!m||!ask)return;
              const base=m[1].endsWith('/doc-flow')?m[1].replace(/\\/doc-flow$/,'/ai'):m[1]+'/ai';
              const url=base+'/manual/'+encodeURIComponent(m[2])+'/perguntar';
              const form=document.getElementById('ask-form'),campo=document.getElementById('ask-pergunta'),saida=document.getElementById('ask-saida'),botao=form.querySelector('button');
              ask.hidden=false;
              function mostrar(texto,classe){const p=document.createElement('p');p.className=classe;p.textContent=texto;saida.replaceChildren(p);return p;}
              form.addEventListener('submit',async event=>{
                event.preventDefault();
                const pergunta=campo.value.trim();
                if(pergunta.length<3)return;
                botao.disabled=true;mostrar('Procurando no manual…','ask-aviso');
                try{
                  const resposta=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({pergunta})});
                  if(!resposta.ok)throw new Error(resposta.status===503?'IA indisponível':'falha');
                  const dados=await resposta.json();
                  mostrar(dados.resposta,dados.modo==='NAO_SEI'?'ask-resposta ask-resposta--nao-sei':'ask-resposta');
                  if(dados.citacoes&&dados.citacoes.length){
                    const lista=document.createElement('ul');lista.className='ask-citacoes';
                    dados.citacoes.forEach(c=>{const li=document.createElement('li'),b=document.createElement('button');b.type='button';b.textContent=c.titulo+(c.secao?' — '+c.secao:'')+' ('+c.codigoTela+')';b.addEventListener('click',()=>{const page=pages.find(p=>p.dataset.codigoTela===c.codigoTela);if(page)openPage(page.id);});li.append(b);lista.append(li);});
                    saida.append(lista);
                  }
                  if(dados.modo!=='NAO_SEI'){const aviso=document.createElement('p');aviso.className='ask-aviso';aviso.textContent='Resposta baseada no '+dados.manual+'. Confira na página citada.';saida.append(aviso);}
                }catch(erro){mostrar('Não foi possível perguntar agora. Use a busca ao lado.','ask-aviso');}
                finally{botao.disabled=false;}
              });
            })();
          </script>
        </body>
        </html>
        """.formatted(HtmlUtils.htmlEscape(cliente.getNome()), cssVariaveisRaizPreviewCliente(cliente),
        empresaBrandBlock, menu, content);
  }

  private String cssVariaveisRaizPreviewCliente(Cliente cliente) {
    String ac = cliente.getTemaCorPrimariaOuPadrao();
    String bg = cliente.getTemaCorFundoOuPadrao();
    String hover = accentHover(ac);
    // Parênteses: sem eles o .formatted só alcança o último trecho e as cores saem trocadas.
    return ("--font-ui:Roboto,system-ui,\"Segoe UI\",Arial,sans-serif;"
        + "--font-body:Roboto,system-ui,\"Segoe UI\",Arial,sans-serif;"
        + "--border:#e5e8ee;--bg:%s;--surface:#fff;--text:#0b1220;--muted:#6b7280;"
        + "--accent:%s;--accent-hover:%s;--accent-soft:%s")
            .formatted(bg, ac, hover, Cliente.TEMA_COR_SOFT_PADRAO);
  }

  private String temaStyleOverridePacoteHtml(Cliente cliente) {
    String ac = cliente.getTemaCorPrimariaOuPadrao();
    String bg = cliente.getTemaCorFundoOuPadrao();
    String hover = accentHover(ac);
    return "<style>:root{--accent:%s!important;--accent-hover:%s!important;--bg:%s!important;--accent-soft:%s!important}</style>"
        .formatted(ac, hover, bg, Cliente.TEMA_COR_SOFT_PADRAO);
  }

  private static String accentHover(String accent) {
    return Cliente.TEMA_COR_PRIMARIA_PADRAO.equalsIgnoreCase(accent)
        ? Cliente.TEMA_COR_PRIMARIA_HOVER_PADRAO
        : accent;
  }

  private String template(Cliente cliente, String versao, String title, String breadcrumb, String menu, String content,
      String assetBase) {
    String brandHtml = buildEmpresaBrandHtml(versao, assetBase);
    String clienteLogoHtml = buildClienteLogoHtml(cliente, assetBase);
    String assetQuery =
        "rev=" + MANUAL_ASSETS_REVISION + "&ver=" + URLEncoder.encode(versao, StandardCharsets.UTF_8);
    String temaTag = temaStyleOverridePacoteHtml(cliente);
    String temaMetaCor = HtmlUtils.htmlEscape(cliente.getTemaCorPrimariaOuPadrao());
    String articleClass = content.contains("welcome-hero") ? "article--welcome" : "";
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
          <script defer src="%s/routes.js?%s"></script>
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
            <article class="%s">
              <header class="article-head">%s</header>
              <div class="article-body">%s</div>
            </article>
          </main>
        </body>
        </html>
        """.formatted(HtmlUtils.htmlEscape(MANUAL_ASSETS_REVISION), temaMetaCor, HtmlUtils.htmlEscape(title),
        assetBase, assetBase, assetQuery, temaTag, assetBase, assetQuery, assetBase, assetQuery, brandHtml, menu,
        HtmlUtils.htmlEscape(breadcrumb), articleClass, clienteLogoHtml, content);
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

  private String menuHtml(List<Pagina> paginas, String pagePrefix, String currentSlug) {
    Map<String, List<Pagina>> grupos = new LinkedHashMap<>();
    for (Pagina pagina : paginas) {
      grupos.computeIfAbsent(pagina.getModulo().getNome(), ignored -> new java.util.ArrayList<>()).add(pagina);
    }
    StringBuilder html = new StringBuilder();
    grupos.forEach((modulo, items) -> {
      boolean tituloRedundante = items.size() == 1
          && modulo.trim().equalsIgnoreCase(items.getFirst().getTitulo().trim());
      Map<UUID, List<Pagina>> filhos = filhosPorParent(items);
      html.append("<section>");
      if (!tituloRedundante) {
        html.append("<h2>").append(HtmlUtils.htmlEscape(modulo)).append("</h2>");
      }
      appendMenuItems(html, roots(items), filhos, pagePrefix, currentSlug);
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
      String pagePrefix, String currentSlug) {
    html.append("<ul>");
    for (Pagina pagina : paginas) {
      List<Pagina> children = filhos.getOrDefault(pagina.getId(), List.of());
      boolean active = currentSlug != null && currentSlug.equals(pagina.getSlug());
      boolean open = !children.isEmpty() && contemSlug(pagina, currentSlug, filhos);
      html.append("<li");
      if (!children.isEmpty()) {
        html.append(" class=\"nav-item has-children").append(open ? " is-open" : "").append('"');
      }
      html.append('>');
      if (!children.isEmpty()) {
        html.append("<div class=\"nav-row\">");
      }
      html.append("<a href=\"").append(pagePrefix).append(pagina.getSlug()).append(".html\"")
          .append(active ? " class=\"active\" aria-current=\"page\"" : "")
          .append(" data-slug=\"").append(HtmlUtils.htmlEscape(pagina.getSlug())).append('"')
          .append(" data-codigo-tela=\"")
          .append(HtmlUtils.htmlEscape(pagina.getCodigoTela())).append("\">")
          .append(HtmlUtils.htmlEscape(pagina.getTitulo())).append("</a>");
      if (!children.isEmpty()) {
        html.append("<button type=\"button\" class=\"nav-toggle\" aria-expanded=\"")
            .append(open).append("\" aria-label=\"Expandir ou recolher ")
            .append(HtmlUtils.htmlEscape(pagina.getTitulo())).append("\"></button></div>");
        appendMenuItems(html, children, filhos, pagePrefix, currentSlug);
      }
      html.append("</li>");
    }
    html.append("</ul>");
  }

  private boolean contemSlug(Pagina pagina, String slug, Map<UUID, List<Pagina>> filhos) {
    if (slug == null || slug.isBlank()) {
      return false;
    }
    if (slug.equals(pagina.getSlug())) {
      return true;
    }
    for (Pagina child : filhos.getOrDefault(pagina.getId(), List.of())) {
      if (contemSlug(child, slug, filhos)) {
        return true;
      }
    }
    return false;
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
        --border:#e5e8ee;--bg:#f7f8fa;--surface:#fff;--surface-2:#f7f8fa;--text:#0b1220;--muted:#6b7280;
        --accent:#4f46e5;--accent-hover:#4338ca;--accent-soft:#eef0ff;--nav-hover:#f1f3f4;
        --success:#10b981;--success-soft:#ecfdf5;--info:#3b82f6;--info-soft:#eff6ff;--warn:#f59e0b;--warn-soft:#fffbeb;
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
        .article-body .doc-intro{margin:0 0 28px;padding:22px 24px;border:1px solid #c7d2fe;border-radius:12px;background:linear-gradient(135deg,var(--accent-soft),var(--surface) 78%)}
        .article-body .doc-intro h2{margin:0 0 8px;padding-left:0;border-left:0}.article-body .doc-intro p{margin:0;color:var(--muted)}
        .article-body .doc-kicker{display:inline-block;margin-bottom:9px;color:var(--accent);font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}
        .article-body .doc-section{margin:28px 0}.article-body .doc-section--soft{padding:20px 22px;border:1px solid var(--border);border-radius:12px;background:var(--surface-2)}
        .article-body .table-wrap{margin:14px 0 24px;overflow-x:auto;border:1px solid var(--border);border-radius:12px;box-shadow:0 1px 2px rgba(60,64,67,.14)}
        .article-body table{width:100%;min-width:520px;border:0;border-collapse:collapse;background:var(--surface);font-size:14px}
        .article-body th,.article-body td{padding:13px 15px;border:0;border-bottom:1px solid var(--border);text-align:left;vertical-align:top}
        .article-body th{background:var(--accent-soft);font-family:var(--font-ui);font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase}.article-body tr:last-child td{border-bottom:0}
        .article-body .steps>ol{display:grid;gap:12px;margin:16px 0 0;padding:0;list-style:none;counter-reset:doc-step}.article-body .steps>ol>li{position:relative;min-height:48px;margin:0;padding:12px 16px 12px 58px;border:1px solid var(--border);border-radius:8px;background:var(--surface);counter-increment:doc-step}.article-body .steps>ol>li:before{position:absolute;top:9px;left:12px;display:grid;width:32px;height:32px;place-items:center;border-radius:50%;background:var(--accent);color:#fff;content:counter(doc-step);font-size:12px;font-weight:700}
        .article-body .checklist{display:grid;gap:9px;padding:0;list-style:none}.article-body .checklist li{position:relative;margin:0;padding:4px 0 4px 30px}.article-body .checklist li:before{position:absolute;top:4px;left:0;display:grid;width:21px;height:21px;place-items:center;border-radius:50%;background:var(--success-soft);color:var(--success);content:'✓';font-size:11px;font-weight:700}
        .article-body .callout,.article-body .warning{position:relative;margin:22px 0;padding:16px 18px 16px 54px;border:1px solid #a5b4fc;border-radius:12px;background:var(--info-soft);color:var(--text)}
        .article-body .warning{border-color:#f9d58c;background:var(--warn-soft)}.article-body .callout:before,.article-body .warning:before{position:absolute;top:15px;left:16px;display:grid;width:24px;height:24px;place-items:center;border-radius:50%;background:var(--info);color:#fff;content:'i';font-size:12px;font-weight:700}.article-body .warning:before{background:var(--warn);content:'!'}.article-body .callout p:last-child,.article-body .warning p:last-child{margin-bottom:0}
        .article-body .faq-list{display:grid;gap:12px;margin-top:16px}.article-body .faq-item{padding:17px 19px;border:1px solid var(--border);border-radius:12px;background:var(--surface);box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body .faq-item h3{margin:0 0 7px;color:var(--accent)}.article-body .faq-item p:last-child{margin-bottom:0;color:var(--muted)}
        .article-body .result-card{margin:22px 0;padding:18px 20px;border:1px solid #6ee7b7;border-radius:12px;background:var(--success-soft)}.article-body .result-card strong{display:block;margin-bottom:5px;color:var(--success)}.article-body .result-card p:last-child{margin-bottom:0}
        .article-body .objective-card{position:relative;margin:24px 0 30px;padding:18px 22px 18px 62px;border:2px solid var(--accent);border-radius:12px;background:#fff;box-shadow:0 8px 24px rgba(79,70,229,.09)}.article-body .objective-card:before{position:absolute;top:17px;left:20px;display:grid;width:28px;height:28px;place-items:center;border-radius:50%;background:var(--accent);color:#fff;content:'i';font-weight:700}.article-body .objective-card strong{display:block;color:var(--accent)}.article-body .objective-card p{margin:4px 0 0;color:var(--muted)}
        .article-body .screen-frame{margin:15px 0 26px;padding:12px;border:1px solid #c7d2fe;border-radius:12px;background:var(--surface-2)}.article-body .screen-frame img{width:100%;margin:0}.article-body .screen-frame figcaption{padding:10px 5px 2px;color:var(--muted);font-size:12px;text-align:center}.article-body .screen-placeholder{display:grid;min-height:260px;padding:30px;place-content:center;border:2px dashed #a5b4fc;border-radius:9px;background:#fff;color:var(--muted);text-align:center}.article-body .screen-placeholder strong{display:block;color:var(--accent)}.article-body .screen-placeholder span{font-size:12px}.article-body .screen-placeholder--compact{min-height:150px;padding:20px;border-width:1px}
        .article-body .content-grid,.article-body .annotation-grid,.article-body .screen-grid{display:grid;gap:16px}.article-body .content-grid--2{grid-template-columns:repeat(2,minmax(0,1fr))}.article-body .content-grid--3,.article-body .annotation-grid,.article-body .screen-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.article-body .annotation-grid{counter-reset:annotation}.article-body .annotation-card,.article-body .rule-card,.article-body .guide-card,.article-body .topic-card{position:relative;padding:18px;border:1px solid var(--border);border-radius:12px;background:#fff;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body .annotation-card{padding-left:58px;counter-increment:annotation}.article-body .annotation-card:before{position:absolute;top:17px;left:17px;display:grid;width:28px;height:28px;place-items:center;border-radius:50%;background:var(--accent);color:#fff;content:counter(annotation);font-size:12px;font-weight:700}.article-body .rule-card{border-top:3px solid var(--accent)}.article-body .annotation-card h3,.article-body .rule-card h2,.article-body .rule-card h3,.article-body .guide-card h3,.article-body .topic-card h3{margin:0 0 7px;padding:0;border:0}.article-body .guide-card{padding:12px 12px 20px}.article-body .guide-card__number{position:absolute;z-index:1;top:22px;left:22px;display:grid;width:32px;height:32px;place-items:center;border:3px solid #fff;border-radius:50%;background:var(--accent);color:#fff;font-weight:700}.article-body .topic-card{text-align:center}.article-body .topic-card__icon{display:grid;width:46px;height:46px;margin:0 auto 14px;place-items:center;border-radius:12px;background:var(--accent-soft);color:var(--accent);font-weight:700}
        .article-body .flow-strip{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;margin:18px 0 28px;padding:0;list-style:none;counter-reset:flow}.article-body .flow-strip li{position:relative;min-height:118px;margin:0;padding:54px 16px 16px;border:1px solid var(--border);border-radius:12px;background:#fff;text-align:center;counter-increment:flow}.article-body .flow-strip li:before{position:absolute;top:14px;left:50%;display:grid;width:30px;height:30px;place-items:center;transform:translateX(-50%);border-radius:50%;background:var(--accent);color:#fff;content:counter(flow);font-weight:700}.article-body .flow-strip li:not(:last-child):after{position:absolute;top:48px;right:-19px;color:var(--accent);content:'→';font-size:20px;font-weight:700}.article-body .flow-strip strong,.article-body .flow-strip span{display:block}.article-body .flow-strip span{color:var(--muted);font-size:12px}.article-body .number-badge{display:grid;width:26px;height:26px;place-items:center;border-radius:50%;background:var(--accent);color:#fff;font-size:11px;font-weight:700}.article-body .status-badge{display:inline-flex;padding:3px 9px;border-radius:999px;background:var(--surface-2);color:var(--muted);font-size:11px;font-weight:700}.article-body .status-badge--required,.article-body .status-badge--sim{background:var(--accent-soft);color:var(--accent)}.article-body .status-badge--ok,.article-body .status-badge--ativo{background:var(--success-soft);color:var(--success)}.article-body .status-badge--nao,.article-body .status-badge--off,.article-body .status-badge--inativo{background:#f3f4f6;color:#6b7280}.article-body .status-badge--warn{background:var(--warn-soft);color:var(--warn)}.article-body .status-badge--danger{background:#fef2f2;color:#ef4444}.article-body .callout--danger{border-color:#fecaca;background:#fef2f2}.article-body .callout--danger:before{background:#ef4444;content:'!'}.article-body .condition-stack{display:grid;grid-template-columns:1fr 1fr;gap:16px}.article-body .condition-block{padding:18px 20px;border:1px solid var(--border);border-radius:12px;background:#fff;box-shadow:0 1px 2px rgba(60,64,67,.14)}.article-body .condition-block__label{display:inline-flex;margin-bottom:10px;padding:3px 10px;border-radius:999px;background:var(--surface-2);color:var(--muted);font-size:11px;font-weight:800;letter-spacing:.08em}.article-body .condition-block--then .condition-block__label{background:var(--accent);color:#fff}.article-body .condition-block h3{margin:0 0 8px}.article-body .condition-block p{margin:0;color:var(--muted)}.article-body .decision-board{display:grid;grid-template-columns:1fr 1.2fr;gap:16px}.article-body .annotation-grid--1,.article-body .content-grid--1{grid-template-columns:minmax(0,1fr)}.article-body .annotation-grid--half{grid-template-columns:minmax(0,.5fr)}.article-body .annotation-grid--3{grid-template-columns:repeat(3,minmax(0,1fr))}
        .article-body .doc-intro--center{text-align:center}.article-body .filter-chips p{display:flex;flex-wrap:wrap;gap:9px;margin:0}.article-body .filter-chip{display:inline-flex;padding:7px 16px;border:1px solid var(--border);border-radius:999px;color:var(--muted);font-size:12px;font-weight:700}.article-body .filter-chip--active{border-color:var(--accent);background:var(--accent);color:#fff}.article-body .resource-list{display:grid;overflow:hidden;border:1px solid var(--border);border-radius:12px;background:#fff}.article-body .resource-item{display:flex;padding:12px 15px;align-items:center;gap:12px;border-bottom:1px solid var(--border)}.article-body .resource-item:last-child{border-bottom:0}.article-body .resource-item>span:not(.number-badge){display:grid;min-width:0;flex:1}.article-body .resource-item strong{color:var(--accent)}.article-body .resource-item small,.article-body .resource-item>span:last-child{color:var(--muted);font-size:11px}.article-body .resource-item__meta{flex:0 0 auto!important;text-align:right}.article-body .rank-list{display:grid;gap:9px;padding:0;list-style:none;counter-reset:rank}.article-body .rank-list li{position:relative;margin:0;padding-left:34px;counter-increment:rank}.article-body .rank-list li:before{position:absolute;left:0;display:grid;width:24px;height:24px;place-items:center;border:1px solid #a5b4fc;border-radius:50%;color:var(--accent);content:counter(rank);font-size:11px;font-weight:700}
        .article-body .journey-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:16px}.article-body .journey-card{position:relative;min-height:178px;padding:54px 17px 18px;border:1px solid var(--border);border-radius:12px;background:#fff;text-align:center}.article-body .journey-card:not(:last-child):after{position:absolute;top:50%;right:-15px;color:var(--accent);content:'→';font-weight:700}.article-body .journey-card__number{position:absolute;top:15px;left:15px;display:grid;width:27px;height:27px;place-items:center;border-radius:50%;background:var(--accent);color:#fff;font-size:11px;font-weight:700}.article-body .journey-card h3{margin:0 0 8px;color:var(--accent)}.article-body .journey-card p{margin:0;color:var(--muted);font-size:12px}.article-body .status-list{display:grid;padding:0;overflow:hidden;border:1px solid var(--border);border-radius:8px;list-style:none}.article-body .status-item{display:flex;margin:0;padding:10px 13px;justify-content:space-between;gap:12px;border-bottom:1px solid var(--border)}.article-body .status-item:last-child{border-bottom:0}.article-body .status-item strong{color:var(--muted);font-size:11px}.article-body .status-item--done strong{color:var(--success)}.article-body .status-item--progress strong{color:var(--warn)}.article-body .related-links{margin-top:26px;padding-top:18px;border-top:1px solid var(--border)}.article-body .related-links p{display:flex;flex-wrap:wrap;gap:10px 24px;margin:0}.article-body .related-links strong{flex-basis:100%}.article-body .related-links span{color:var(--accent);font-size:12px;font-weight:700}.article-body .related-links span:after{margin-left:7px;content:'→'}.article-body [data-codigo-tela],.article-content [data-codigo-tela]{cursor:pointer;color:var(--accent);font-weight:700}.article-body [data-codigo-tela]:hover,.article-content [data-codigo-tela]:hover{text-decoration:underline}.article-body .related-links [data-codigo-tela],.article-content .related-links [data-codigo-tela]{display:inline}
        .article-body pre,.article-body code{font-family:ui-monospace,"Roboto Mono",Menlo,monospace}
        .article-body pre{overflow:auto;background:#202124;color:#e8eaed;padding:16px 18px;border-radius:8px;font-size:13px;line-height:1.55}
        .article-body img{max-width:100%;height:auto;display:block;border-radius:8px;margin:16px 0}
        .article-body figure.photo{margin:16px 0}.article-body figure.photo figcaption{font-size:12px;color:var(--muted);margin-top:8px;font-family:var(--font-ui)}
        .article-body h1,.article-body h2,.article-body h3{font-family:var(--font-body);font-weight:600;color:var(--text);line-height:1.3;margin:1.35em 0 .55em;letter-spacing:-0.01em}
        .article-body h2{padding-left:14px;border-left:4px solid var(--accent)}
        .article-body h1{font-size:1.5rem}.article-body h2{font-size:1.25rem}.article-body h3{font-size:1.0625rem}
        .article-body h1:first-child,.article-body h2:first-child{margin-top:0}
        .article-body p{margin:0 0 1em}.article-body li{margin:.3em 0}
        .cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px}
        .page-card{display:block;border:1px solid var(--border);border-radius:8px;padding:16px 18px;color:inherit;text-decoration:none;background:var(--surface);transition:box-shadow .2s;border-color:var(--border)}
        .page-card:hover{box-shadow:0 1px 2px 0 rgba(60,64,67,.3),0 2px 6px 2px rgba(60,64,67,.15)}
        .page-card span,.page-card small{display:block;color:var(--muted);font-size:12px;font-family:var(--font-ui)}.page-card strong{display:block;margin:8px 0 4px;font-size:15px;color:var(--text);font-family:var(--font-ui);font-weight:600}
        .welcome-hero{position:relative;overflow:hidden;margin:-28px -40px 24px;padding:52px clamp(24px,5vw,72px) 42px;border-bottom:1px solid #c7d2fe;background:linear-gradient(118deg,#eef0ff 0%%,#f5f7ff 52%%,#fff 100%%)}.welcome-hero:before{position:absolute;right:-95px;top:-150px;width:370px;height:370px;border:50px solid rgba(79,70,229,.09);border-radius:50%%;content:''}.welcome-hero__glow{position:absolute;right:13%%;bottom:-125px;width:260px;height:260px;border-radius:50%%;background:rgba(79,70,229,.1);filter:blur(2px)}.welcome-hero>*:not(.welcome-hero__glow){position:relative;z-index:1}.welcome-eyebrow{display:inline-flex;margin-bottom:13px;padding:5px 10px;border:1px solid #a5b4fc;border-radius:999px;background:#fff;color:var(--accent);font-size:10px;font-weight:700;letter-spacing:.09em}.welcome-hero h1{max-width:650px;margin:0 0 10px;padding:0;border:0;font-size:clamp(28px,3.2vw,42px);letter-spacing:-.035em}.welcome-hero p{max-width:650px;margin:0;color:var(--muted);font-size:16px;line-height:1.6}.welcome-search{display:flex;max-width:650px;margin:26px 0 0;padding:0 16px;align-items:center;gap:11px;border:1px solid #a5b4fc;border-radius:12px;background:#fff;box-shadow:0 8px 22px rgba(79,70,229,.11);color:var(--accent)}.welcome-search span{font-size:26px;line-height:1;transform:rotate(-20deg)}.welcome-search input{width:100%;height:54px;border:0;outline:0;background:transparent;color:var(--text);font:inherit;font-size:15px}.welcome-search input::placeholder{color:#80868b}.welcome-search:focus-within{border-color:var(--accent);box-shadow:0 0 0 3px rgba(79,70,229,.18),0 8px 22px rgba(79,70,229,.11)}.welcome-search-status{min-height:21px;margin-top:8px!important;font-size:12px!important}.welcome-hero__actions{display:flex;flex-wrap:wrap;gap:11px;margin-top:21px}.welcome-button{display:inline-flex;padding:10px 15px;align-items:center;gap:10px;border:1px solid #a5b4fc;border-radius:8px;background:#fff;color:var(--accent);font-family:var(--font-ui);font-size:13px;font-weight:700;text-decoration:none}.welcome-button:hover{border-color:var(--accent);background:var(--accent-soft);text-decoration:none}.welcome-button--primary{border-color:var(--accent);background:var(--accent);color:#fff}.welcome-button--primary:hover{background:var(--accent-hover);color:#fff}.welcome-button span{font-size:17px}.welcome-overview{display:grid;grid-template-columns:repeat(3,1fr);margin:0 0 30px;overflow:hidden;border:1px solid var(--border);border-radius:12px;background:#fff;box-shadow:0 2px 5px rgba(60,64,67,.08)}.welcome-overview>div,.welcome-overview>a{display:flex;min-height:82px;padding:17px 20px;flex-direction:column;justify-content:center;border-right:1px solid var(--border);color:inherit;text-decoration:none}.welcome-overview>a{border-right:0;background:var(--surface-2)}.welcome-overview strong{color:var(--accent);font-family:var(--font-ui);font-size:23px;line-height:1.1}.welcome-overview span{margin-top:5px;color:var(--muted);font-family:var(--font-ui);font-size:12px}.welcome-overview>a strong{font-size:16px}.welcome-paths{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:38px}.welcome-path{display:flex;min-height:100px;padding:17px;align-items:center;gap:13px;border:1px solid var(--border);border-radius:12px;background:#fff;color:var(--text);text-decoration:none;transition:transform .15s,box-shadow .15s,border-color .15s}.welcome-path:hover{border-color:#a5b4fc;box-shadow:0 5px 16px rgba(60,64,67,.13);transform:translateY(-2px);text-decoration:none}.welcome-path__icon{display:grid;width:33px;height:33px;flex:0 0 33px;place-items:center;border-radius:10px;background:var(--accent-soft);color:var(--accent);font-family:var(--font-ui);font-size:15px;font-weight:700}.welcome-path>span:nth-child(2){display:grid;min-width:0;flex:1}.welcome-path strong{font-family:var(--font-ui);font-size:13px}.welcome-path small{overflow:hidden;color:var(--muted);font-size:11px;text-overflow:ellipsis;white-space:nowrap}.welcome-path>b{color:var(--accent);font-size:18px}.welcome-guides{padding-top:2px}.welcome-section-heading{display:flex;margin-bottom:17px;align-items:end;justify-content:space-between;gap:20px}.welcome-section-heading span{display:block;margin-bottom:5px;color:var(--accent);font-family:var(--font-ui);font-size:10px;font-weight:700;letter-spacing:.09em}.welcome-section-heading h2{margin:0;padding:0;border:0;font-size:22px}.welcome-section-heading p{margin:0;color:var(--muted);font-size:12px}.welcome-pages{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:13px}.welcome-page-card{display:flex;min-height:166px;padding:18px;flex-direction:column;border:1px solid var(--border);border-radius:12px;background:#fff;color:var(--text);text-decoration:none;transition:transform .15s,box-shadow .15s,border-color .15s}.welcome-page-card:hover{border-color:#a5b4fc;box-shadow:0 6px 17px rgba(60,64,67,.13);transform:translateY(-2px);text-decoration:none}.welcome-page-card__module{overflow:hidden;color:var(--accent);font-family:var(--font-ui);font-size:11px;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.welcome-page-card strong{margin:7px 0;color:var(--text);font-family:var(--font-ui);font-size:15px;line-height:1.35}.welcome-page-card small{display:-webkit-box;overflow:hidden;color:var(--muted);font-size:12px;line-height:1.45;-webkit-box-orient:vertical;-webkit-line-clamp:2}.welcome-page-card__action{display:block;margin-top:auto;padding-top:13px;color:var(--accent);font-family:var(--font-ui);font-size:12px;font-weight:700}.welcome-page-card__action b{margin-left:5px;font-size:15px}.welcome-empty{display:grid;padding:40px 20px;place-items:center;border:1px dashed #a5b4fc;border-radius:12px;background:var(--accent-soft);color:var(--muted);text-align:center}.welcome-empty strong{color:var(--accent)}.welcome-empty span{font-size:12px}
        .article--welcome .article-head{display:none}.article-body .welcome-button--primary,.article-body .welcome-button--primary:hover{color:#fff}
        #manual-search{width:100%;height:40px;border:1px solid var(--border);border-radius:8px;padding:0 14px;margin-bottom:12px;font-size:14px;font-family:var(--font-ui);background:var(--surface);color:var(--text)}
        #manual-search:focus{outline:none;border-color:var(--accent);box-shadow:0 0 0 2px rgba(79,70,229,.25)}
        .search-results{display:grid;gap:8px;margin:0 0 16px}
        .search-result{display:block;border:1px solid var(--border);border-radius:8px;padding:10px 14px;color:inherit;text-decoration:none;background:var(--surface)}
        .search-result:hover{border-color:var(--accent);background:var(--accent-soft)}
        .search-result strong{display:block;font-size:14px;font-family:var(--font-ui);font-weight:500}.search-result span{display:block;color:var(--muted);font-size:12px;margin-top:4px}
        nav section{margin:0 0 20px}
        nav h2{font-size:12px;font-weight:500;line-height:16px;color:var(--muted);text-transform:none;letter-spacing:.1px;margin:0 0 8px 16px}
        nav ul{list-style:none;margin:0;padding:0}nav section>ul{padding-left:0}nav li{margin:0}
        nav ul ul{margin:4px 0 0;padding-left:10px;border-left:2px solid #e8eaed}
        nav .nav-row{display:flex;align-items:center;gap:2px;min-width:0}
        nav .nav-row>a{flex:1;min-width:0}
        nav .nav-toggle{flex:0 0 28px;width:28px;height:28px;margin:0 4px 0 0;padding:0;border:0;border-radius:999px;background:transparent;color:var(--muted);cursor:pointer;position:relative}
        nav .nav-toggle:hover{background:var(--nav-hover);color:var(--text)}
        nav .nav-toggle:after{content:'';position:absolute;top:50%;left:50%;width:.4em;height:.4em;border-right:1.5px solid currentColor;border-bottom:1.5px solid currentColor;transform:translate(-50%,-65%) rotate(45deg)}
        nav li.has-children.is-open>.nav-row>.nav-toggle:after{transform:translate(-50%,-35%) rotate(225deg)}
        nav li.has-children:not(.is-open)>ul{display:none}
        nav a{display:block;color:#3c4043;text-decoration:none;padding:10px 16px;border-radius:999px;font-size:14px;font-weight:400;line-height:20px;transition:background .12s ease,color .12s ease}
        nav a:hover{background:var(--nav-hover);color:var(--text)}
        nav a.active{background:var(--accent-soft);color:var(--accent-hover);font-weight:500}
        .manual-tela-ausente{margin:0 0 16px;padding:12px 16px;border:1px solid #fcd34d;border-radius:12px;background:#fffbeb;color:#92400e;font-size:14px}
        @media(max-width:820px){body{display:block;grid-template-columns:1fr}.sidebar{height:auto;position:relative;max-height:none;box-shadow:none}.content{padding:18px}.article-body{padding:22px}.welcome-hero{margin:-22px -22px 22px;padding:36px 24px}.welcome-overview,.welcome-paths{grid-template-columns:1fr}.welcome-overview>div,.welcome-overview>a{min-height:68px;border-right:0;border-bottom:1px solid var(--border)}.welcome-overview>a{border-bottom:0}.welcome-section-heading{align-items:flex-start;flex-direction:column;gap:5px}.article-body .content-grid--2,.article-body .content-grid--3,.article-body .annotation-grid,.article-body .screen-grid,.article-body .flow-strip,.article-body .journey-grid,.article-body .annotation-grid--2,.article-body .annotation-grid--3,.article-body .annotation-grid--half{grid-template-columns:1fr}.article-body .flow-strip li:not(:last-child):after,.article-body .journey-card:not(:last-child):after{top:auto;right:50%;bottom:-25px;transform:translateX(50%) rotate(90deg)}.article-body .resource-item{align-items:flex-start;flex-wrap:wrap}.article-body .resource-item__meta{flex-basis:100%!important;text-align:left}}
        """.replace("%%", "%"));
    Files.writeString(assetsDir.resolve("app.js"), """
        const manualRootBase = window.location.pathname.includes('/paginas/') ? '..' : '.';
        async function manualRoutes(basePath = manualRootBase) {
          if (window.MANUAL_ROUTES) return window.MANUAL_ROUTES;
          const response = await fetch(`${basePath}/routes.json`);
          return response.json();
        }
        async function manualOpenByCodigoTela(codigoTela, basePath = manualRootBase) {
          const routes = await manualRoutes(basePath);
          const url = routes[codigoTela];
          if (url) window.open(`${basePath}/${url}`, '_blank');
          return url;
        }
        window.manualOpenByCodigoTela = manualOpenByCodigoTela;
        function manualNormalize(text) {
          return (text || '').toString().normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase();
        }
        /** Mesma regra de ManualSinonimos.normalizar: sem acento, minúsculo, pontuação vira espaço. */
        function manualTermos(text) {
          return manualNormalize(text).replace(/[^a-z0-9]+/g, ' ').trim();
        }
        let manualSinonimos = [];
        fetch(`${manualRootBase}/sinonimos.json`, { cache: 'no-store' })
          .then(response => response.ok ? response.json() : { grupos: [] })
          .then(json => { manualSinonimos = Array.isArray(json.grupos) ? json.grupos : []; })
          .catch(() => undefined);
        /** A busca e as trocas por sinônimo ("nf" → "nota fiscal"), só com palavra inteira. */
        function manualAlternativas(term) {
          const base = ` ${manualTermos(term)} `;
          const alternativas = [base.trim()];
          manualSinonimos.forEach(grupo => grupo.forEach(termo => {
            if (!termo || !base.includes(` ${termo} `)) return;
            grupo.forEach(outro => {
              const alternativa = base.replace(` ${termo} `, ` ${outro} `).trim();
              if (outro !== termo && !alternativas.includes(alternativa) && alternativas.length < 8) {
                alternativas.push(alternativa);
              }
            });
          }));
          return alternativas.filter(Boolean);
        }
        /** O que o leitor digitou casa em qualquer ponto (como sempre); a troca, só no começo de palavra. */
        function manualCombina(text, alternativas) {
          const alvo = ` ${manualTermos(text)}`;
          return alternativas.some((alternativa, i) => alvo.includes(i === 0 ? alternativa : ` ${alternativa}`));
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
        function manualNavLink(li) {
          return li.querySelector(':scope > a, :scope > .nav-row > a');
        }
        function manualFilterNav(term) {
          const normalizedTerm = manualNormalize(term);
          document.querySelectorAll('nav section').forEach(section => {
            const items = [...section.querySelectorAll('li')].reverse();
            let sectionVisible = false;
            items.forEach(li => {
              const link = manualNavLink(li);
              const text = `${link?.textContent || ''} ${link?.dataset.codigoTela || ''}`;
              const selfMatch = !normalizedTerm || manualNormalize(text).includes(normalizedTerm);
              const childVisible = [...li.querySelectorAll(':scope > ul > li')]
                .some(child => child.style.display !== 'none');
              const show = selfMatch || childVisible;
              li.style.display = show ? '' : 'none';
              if (show && normalizedTerm && childVisible) {
                li.classList.add('is-open');
                li.querySelector(':scope > .nav-row .nav-toggle')?.setAttribute('aria-expanded', 'true');
              }
              if (show) sectionVisible = true;
            });
            section.style.display = sectionVisible || !normalizedTerm ? '' : 'none';
          });
        }
        function manualRestoreNavOpenState() {
          document.querySelectorAll('nav section').forEach(section => {
            section.style.display = '';
          });
          document.querySelectorAll('nav li').forEach(li => {
            li.style.display = '';
            const hasActive = !!li.querySelector('a.active');
            if (li.classList.contains('has-children')) {
              li.classList.toggle('is-open', hasActive);
              li.querySelector(':scope > .nav-row .nav-toggle')
                ?.setAttribute('aria-expanded', hasActive ? 'true' : 'false');
            }
          });
        }
        /** INT-605: hospedado em /manual/{chave}/site/, registra buscas e páginas abertas (sem identificar o leitor). */
        const manualEventosUrl = (() => {
          const m = location.pathname.match(/^(.*\\/manual\\/[^/]+)\\/site\\//);
          return m ? `${m[1]}/eventos` : null;
        })();
        function manualRegistrar(evento) {
          if (!manualEventosUrl) return;
          try {
            fetch(manualEventosUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(evento), keepalive: true }).catch(() => undefined);
          } catch (e) { /* uso do manual não pode quebrar a leitura */ }
        }
        let manualBuscaTimer = null;
        function manualRegistrarBusca(termo, resultados) {
          clearTimeout(manualBuscaTimer);
          if (!termo || termo.length < 3) return;
          manualBuscaTimer = setTimeout(() => manualRegistrar({
            tipo: resultados > 0 ? 'BUSCA' : 'BUSCA_SEM_RESULTADO', termo, resultados }), 1200);
        }
        document.addEventListener('DOMContentLoaded', () => {
          const codigo = document.querySelector('meta[name="docflow-codigo-tela"]')?.content;
          if (codigo) manualRegistrar({ tipo: 'PAGINA_ABERTA', codigoTela: codigo });
        });
        /** Deep link por código de tela (INT-104): index.html?tela=PED-001 ou #tela=PED-001. */
        function manualDeepLink() {
          const hash = location.hash.startsWith('#tela=') ? decodeURIComponent(location.hash.slice(6)) : null;
          const codigo = (new URLSearchParams(location.search).get('tela') || hash || '').trim();
          if (!codigo) return;
          const routes = window.MANUAL_ROUTES || {};
          const url = routes[codigo] || routes[codigo.toUpperCase()];
          if (url) {
            if (!location.pathname.endsWith(url)) location.replace(`${manualRootBase}/${url}`);
            return;
          }
          const artigo = document.querySelector('main.content > article');
          if (!artigo) return;
          const aviso = document.createElement('div');
          aviso.className = 'manual-tela-ausente';
          aviso.setAttribute('role', 'status');
          aviso.textContent = `A tela ${codigo} não está nesta versão do manual. Use a busca ao lado para encontrar o assunto.`;
          artigo.before(aviso);
        }
        document.addEventListener('DOMContentLoaded', manualDeepLink);
        document.addEventListener('DOMContentLoaded', () => {
          const file = decodeURIComponent((location.pathname.split('/').pop() || ''));
          document.querySelectorAll('nav a[href]').forEach(a => {
            const hrefFile = (a.getAttribute('href') || '').split('/').pop();
            if (hrefFile && hrefFile === file) {
              a.classList.add('active');
              a.setAttribute('aria-current', 'page');
              let node = a.closest('li');
              while (node) {
                if (node.classList.contains('has-children')) {
                  node.classList.add('is-open');
                  node.querySelector(':scope > .nav-row .nav-toggle')?.setAttribute('aria-expanded', 'true');
                }
                node = node.parentElement?.closest('li');
              }
            }
          });

          document.querySelectorAll('[data-codigo-tela]').forEach(el => {
            if (el.tagName === 'A' && el.getAttribute('href') && el.getAttribute('href') !== '#') return;
            el.setAttribute('role', el.getAttribute('role') || 'link');
            el.setAttribute('tabindex', el.getAttribute('tabindex') || '0');
            const open = () => {
              const codigo = el.getAttribute('data-codigo-tela');
              if (!codigo || /^CODIGO/i.test(codigo.trim())) return;
              manualOpenByCodigoTela(codigo.trim()).catch(() => undefined);
            };
            el.addEventListener('click', event => {
              event.preventDefault();
              open();
            });
            el.addEventListener('keydown', event => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                open();
              }
            });
          });

          document.querySelectorAll('nav .nav-toggle').forEach(btn => {
            btn.addEventListener('click', event => {
              event.preventDefault();
              event.stopPropagation();
              const li = btn.closest('li');
              if (!li) return;
              const open = li.classList.toggle('is-open');
              btn.setAttribute('aria-expanded', open ? 'true' : 'false');
            });
          });
          const input = document.getElementById('manual-search');
          if (input) {
            const results = document.createElement('div');
            results.id = 'manual-search-results';
            results.className = 'search-results';
            input.insertAdjacentElement('afterend', results);
            let searchIndex = [];
            manualLoadSearchIndex().then(index => searchIndex = index).catch(() => searchIndex = []);
            input.addEventListener('input', () => {
              const term = input.value.trim();
              const normalizedTerm = manualNormalize(term);
              if (!normalizedTerm) {
                manualRestoreNavOpenState();
              } else {
                manualFilterNav(term);
              }
              if (normalizedTerm.length < 2) {
                results.replaceChildren();
                return;
              }
              const alternativas = manualAlternativas(term);
              const encontrados = searchIndex
                .filter(item => manualCombina(`${item.titulo} ${item.codigoTela} ${item.texto}`, alternativas));
              manualRegistrarBusca(term, encontrados.length);
              const matches = encontrados
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
          }
          const welcomeInput = document.getElementById('welcome-search');
          if (welcomeInput) {
            const cards = [...document.querySelectorAll('[data-welcome-page]')];
            const empty = document.getElementById('welcome-empty');
            const status = document.getElementById('welcome-search-status');
            welcomeInput.addEventListener('input', () => {
              const term = manualNormalize(welcomeInput.value.trim());
              const alternativas = manualAlternativas(welcomeInput.value.trim());
              let visible = 0;
              cards.forEach(card => {
                const match = !term || manualCombina(card.dataset.search, alternativas);
                card.hidden = !match;
                if (match) visible += 1;
              });
              empty.hidden = visible > 0;
              manualRegistrarBusca(welcomeInput.value.trim(), visible);
              status.textContent = term
                ? `${visible} ${visible === 1 ? 'guia encontrado' : 'guias encontrados'} para “${welcomeInput.value.trim()}”.`
                : 'Explore os guias disponíveis abaixo.';
            });
          }
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
    urls.add("./assets/routes.js?" + assetQ);
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
          // Sinônimos mudam sem nova publicação: rede primeiro, cache só sem conexão.
          if (new URL(event.request.url).pathname.endsWith('/sinonimos.json')) {
            event.respondWith(fetch(event.request).then(response => {
              const copy = response.clone();
              caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
              return response;
            }).catch(() => caches.match(event.request)));
            return;
          }
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
