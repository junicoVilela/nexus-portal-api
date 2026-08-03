package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaAnexo;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Monta HTML de impressão no tom de manual de negócio (capa, índice por módulo,
 * capítulos com capturas reduzidas). Usado sob demanda no download PDF.
 */
@Service
@RequiredArgsConstructor
public class GeradorManualPdfService {

  private static final DateTimeFormatter DATA_BR =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", Locale.forLanguageTag("pt-BR"));

  private final GeradorPacoteService geradorPacoteService;
  private final PaginaAnexoRepository paginaAnexoRepository;

  public String montarHtmlManual(Cliente cliente, String versao) {
    List<Pagina> paginas = geradorPacoteService.selecionarPaginas(cliente.getId());
    if (paginas.isEmpty()) {
      throw new BusinessException("Não há páginas publicadas elegíveis para este cliente.");
    }
    String versaoSegura = versao == null || versao.isBlank() ? "1.0.0" : versao.trim();
    Map<String, List<Pagina>> porModulo = agruparPorModulo(paginas);
    String capa = montarCapa(cliente, versaoSegura, paginas.size(), porModulo.size());
    String indice = montarIndice(porModulo);
    String capitulos = montarCapitulos(porModulo);
    return template(cliente.getNome(), versaoSegura, capa + indice + capitulos);
  }

  private Map<String, List<Pagina>> agruparPorModulo(List<Pagina> paginas) {
    Map<String, List<Pagina>> grupos = new LinkedHashMap<>();
    for (Pagina pagina : paginas) {
      String nomeModulo = pagina.getModulo() == null || pagina.getModulo().getNome() == null
          ? "Geral"
          : pagina.getModulo().getNome().trim();
      grupos.computeIfAbsent(nomeModulo, ignorado -> new ArrayList<>()).add(pagina);
    }
    return grupos;
  }

  private String montarCapa(Cliente cliente, String versao, int qtdPaginas, int qtdModulos) {
    String data = DATA_BR.format(LocalDate.now());
    return """
        <section class="cover">
          <p class="cover__kicker">Manual do usuário</p>
          <h1 class="cover__title">%s</h1>
          <p class="cover__subtitle">Guia operacional de negócio</p>
          <dl class="cover__meta">
            <div><dt>Versão</dt><dd>%s</dd></div>
            <div><dt>Data</dt><dd>%s</dd></div>
            <div><dt>Conteúdo</dt><dd>%d módulos · %d tópicos</dd></div>
          </dl>
          <p class="cover__note">Documento destinado a orientar processos e decisões operacionais.
          As capturas de tela são ilustrativas e aparecem em tamanho reduzido.</p>
        </section>
        """.formatted(
        esc(cliente.getNome()),
        esc(versao),
        esc(data),
        qtdModulos,
        qtdPaginas);
  }

  private String montarIndice(Map<String, List<Pagina>> porModulo) {
    StringBuilder sb = new StringBuilder();
    sb.append("<section class=\"toc\"><h1>Índice</h1>");
    int moduloIdx = 1;
    for (Map.Entry<String, List<Pagina>> entry : porModulo.entrySet()) {
      sb.append("<div class=\"toc__module\">");
      sb.append("<h2>").append(moduloIdx).append(". ")
          .append(esc(entry.getKey())).append("</h2><ol>");
      int item = 1;
      for (Pagina pagina : entry.getValue()) {
        sb.append("<li><a href=\"#")
            .append(esc(ancora(pagina)))
            .append("\">")
            .append(moduloIdx).append('.').append(item).append(' ')
            .append(esc(pagina.getTitulo()))
            .append("</a>");
        if (pagina.getResumo() != null && !pagina.getResumo().isBlank()) {
          sb.append("<span class=\"toc__resumo\">")
              .append(esc(pagina.getResumo().trim()))
              .append("</span>");
        }
        sb.append("</li>");
        item++;
      }
      sb.append("</ol></div>");
      moduloIdx++;
    }
    sb.append("</section>");
    return sb.toString();
  }

  private String montarCapitulos(Map<String, List<Pagina>> porModulo) {
    StringBuilder sb = new StringBuilder();
    int moduloIdx = 1;
    for (Map.Entry<String, List<Pagina>> entry : porModulo.entrySet()) {
      sb.append("<section class=\"module-break\"><h1 class=\"module-title\">")
          .append(moduloIdx).append(". ")
          .append(esc(entry.getKey()))
          .append("</h1></section>");
      int item = 1;
      for (Pagina pagina : entry.getValue()) {
        sb.append(montarCapitulo(pagina, moduloIdx, item));
        item++;
      }
      moduloIdx++;
    }
    return sb.toString();
  }

  private String montarCapitulo(Pagina pagina, int moduloIdx, int item) {
    String resumo = pagina.getResumo() == null || pagina.getResumo().isBlank()
        ? ""
        : "<p class=\"chapter__resumo\">" + esc(pagina.getResumo().trim()) + "</p>";
    String conteudo = prepararConteudoComAnexosInline(pagina);
    if (conteudo.isBlank()) {
      conteudo = "<p>Sem conteúdo cadastrado para este tópico.</p>";
    }
    return """
        <article class="chapter" id="%s">
          <header class="chapter__head">
            <p class="chapter__num">%d.%d</p>
            <h2 class="chapter__title">%s</h2>
            %s
          </header>
          <div class="chapter__body">%s</div>
        </article>
        """.formatted(
        esc(ancora(pagina)),
        moduloIdx,
        item,
        esc(pagina.getTitulo()),
        resumo,
        conteudo);
  }

  private String prepararConteudoComAnexosInline(Pagina pagina) {
    String html = pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml();
    if (html.isBlank()) {
      return "";
    }
    Document document = Jsoup.parseBodyFragment(html);
    document.select(".screen-placeholder").remove();
    document.select("script, style").remove();

    List<PaginaAnexo> anexos = paginaAnexoRepository.findByPagina_Id(pagina.getId());
    for (PaginaAnexo anexo : anexos) {
      String dataUri = dataUriDoAnexo(anexo);
      if (dataUri == null) {
        continue;
      }
      String anexoId = anexo.getId().toString();
      document.select("img[src]").forEach(img -> {
        String src = img.attr("src");
        if (src.contains(anexoId)) {
          img.attr("src", dataUri);
          img.addClass("manual-capture");
          if (!img.hasAttr("alt") || img.attr("alt").isBlank()) {
            img.attr("alt", anexo.getNomeOriginal() == null ? "Captura" : anexo.getNomeOriginal());
          }
        }
      });
    }

    // Capturas sem anexo resolvido ainda entram reduzidas se forem data/http.
    for (Element img : document.select("img[src]")) {
      img.addClass("manual-capture");
    }
    return document.body().html();
  }

  private String dataUriDoAnexo(PaginaAnexo anexo) {
    try {
      Path origem = Path.of(anexo.getCaminho());
      if (!Files.exists(origem)) {
        return null;
      }
      String type = anexo.getContentType() == null || anexo.getContentType().isBlank()
          ? "image/png"
          : anexo.getContentType();
      byte[] bytes = Files.readAllBytes(origem);
      return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (IOException | RuntimeException ex) {
      return null;
    }
  }

  private String ancora(Pagina pagina) {
    String slug = pagina.getSlug() == null || pagina.getSlug().isBlank()
        ? pagina.getId().toString()
        : pagina.getSlug();
    return "topico-" + slug.replaceAll("[^a-zA-Z0-9\\-_]", "-");
  }

  private String template(String clienteNome, String versao, String body) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8"/>
          <title>Manual — __CLIENTE__ — v__VERSAO__</title>
          <style>
            @page { size: A4; margin: 18mm 16mm 18mm 16mm; }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              color: #1f2937;
              font-family: "Georgia", "Times New Roman", Times, serif;
              font-size: 11pt;
              line-height: 1.55;
              background: #fff;
            }
            h1, h2, h3, h4 {
              font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
              color: #111827;
              line-height: 1.25;
              page-break-after: avoid;
            }
            a { color: #1f2937; text-decoration: none; }
            .cover {
              min-height: 220mm;
              padding-top: 28mm;
              page-break-after: always;
            }
            .cover__kicker {
              margin: 0 0 10px;
              color: #4b5563;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 11pt;
              font-weight: 700;
              letter-spacing: 0.12em;
              text-transform: uppercase;
            }
            .cover__title {
              margin: 0 0 8px;
              font-size: 28pt;
              letter-spacing: -0.02em;
            }
            .cover__subtitle {
              margin: 0 0 28px;
              color: #4b5563;
              font-size: 13pt;
            }
            .cover__meta {
              display: table;
              width: 100%;
              margin: 0 0 28px;
              border-top: 1px solid #d1d5db;
              border-bottom: 1px solid #d1d5db;
              padding: 14px 0;
            }
            .cover__meta > div { display: table-row; }
            .cover__meta dt, .cover__meta dd {
              display: table-cell;
              padding: 5px 0;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 10pt;
            }
            .cover__meta dt { width: 28%; color: #6b7280; font-weight: 700; }
            .cover__meta dd { margin: 0; }
            .cover__note {
              max-width: 85%;
              color: #4b5563;
              font-size: 10pt;
            }
            .toc { page-break-after: always; }
            .toc > h1 {
              margin: 0 0 18px;
              padding-bottom: 8px;
              border-bottom: 2px solid #111827;
              font-size: 18pt;
            }
            .toc__module { margin: 0 0 18px; }
            .toc__module h2 {
              margin: 0 0 8px;
              font-size: 12pt;
            }
            .toc ol {
              margin: 0;
              padding-left: 18px;
            }
            .toc li {
              margin: 0 0 7px;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 10pt;
            }
            .toc__resumo {
              display: block;
              margin-top: 2px;
              color: #6b7280;
              font-size: 9pt;
              font-weight: 400;
            }
            .module-break {
              page-break-before: always;
              margin: 0 0 10px;
            }
            .module-title {
              margin: 0;
              padding-bottom: 6px;
              border-bottom: 1px solid #9ca3af;
              font-size: 16pt;
            }
            .chapter {
              page-break-before: always;
              page-break-inside: auto;
            }
            .module-break + .chapter {
              page-break-before: auto;
            }
            .chapter__head { margin: 0 0 14px; }
            .chapter__num {
              margin: 0 0 4px;
              color: #6b7280;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 9pt;
              font-weight: 700;
              letter-spacing: 0.06em;
            }
            .chapter__title {
              margin: 0 0 8px;
              font-size: 15pt;
            }
            .chapter__resumo {
              margin: 0;
              color: #4b5563;
              font-size: 10.5pt;
            }
            .chapter__body > :first-child { margin-top: 0; }
            .chapter__body h2, .chapter__body h3 {
              margin: 1.2em 0 0.45em;
              font-size: 12pt;
            }
            .chapter__body p, .chapter__body li { font-size: 10.5pt; }
            .chapter__body table {
              width: 100%;
              border-collapse: collapse;
              margin: 10px 0 16px;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 9pt;
            }
            .chapter__body th, .chapter__body td {
              border: 1px solid #d1d5db;
              padding: 6px 8px;
              text-align: left;
              vertical-align: top;
            }
            .chapter__body th { background: #f3f4f6; }
            .chapter__body .doc-intro,
            .chapter__body .objective-card,
            .chapter__body .callout,
            .chapter__body .warning,
            .chapter__body .result-card,
            .chapter__body .doc-section--soft {
              margin: 12px 0;
              padding: 10px 12px;
              border: 1px solid #e5e7eb;
              background: #fafafa;
            }
            .chapter__body .doc-kicker {
              display: block;
              margin-bottom: 4px;
              color: #6b7280;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 8pt;
              font-weight: 700;
              letter-spacing: 0.08em;
              text-transform: uppercase;
            }
            .chapter__body .content-grid,
            .chapter__body .annotation-grid,
            .chapter__body .screen-grid,
            .chapter__body .flow-strip,
            .chapter__body .journey-grid,
            .chapter__body .condition-stack,
            .chapter__body .decision-board {
              display: block !important;
            }
            .chapter__body .screen-frame {
              margin: 12px auto 16px;
              padding: 0;
              border: 0;
              background: transparent;
              text-align: center;
              page-break-inside: avoid;
            }
            .chapter__body .screen-frame figcaption,
            .chapter__body figcaption {
              margin-top: 4px;
              color: #6b7280;
              font-family: Helvetica, Arial, sans-serif;
              font-size: 8.5pt;
              text-align: center;
            }
            .chapter__body img.manual-capture,
            .chapter__body .screen-frame img,
            .chapter__body img {
              display: block;
              max-width: 58%;
              max-height: 240px;
              width: auto;
              height: auto;
              margin: 10px auto;
              border: 1px solid #d1d5db;
            }
            .chapter__body .related-links { margin-top: 16px; color: #4b5563; font-size: 9.5pt; }
          </style>
        </head>
        <body>
        __BODY__
        </body>
        </html>
        """
        .replace("__CLIENTE__", esc(clienteNome))
        .replace("__VERSAO__", esc(versao))
        .replace("__BODY__", body);
  }

  private static String esc(String value) {
    return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
  }
}
