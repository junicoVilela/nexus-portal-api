package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.shared.exception.BusinessException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Renderer Markdown → PDF (F0.7). Pipeline:
 *
 * <ol>
 *   <li>Thymeleaf processa o template com as variáveis do contexto.</li>
 *   <li>commonmark-java (+ GFM tables) converte Markdown → HTML.</li>
 *   <li>openhtmltopdf gera o PDF a partir do HTML envolvido em um shell
 *       com CSS de impressão.</li>
 * </ol>
 *
 * <p>O serviço é stateless e pode ser injetado em qualquer endpoint que
 * precise gerar PDF a partir de um template de release (F0.8) ou
 * documento de entrega (spec 25).
 */
@Service
public class MarkdownPdfRenderer {

  private final TemplateEngine thymeleaf;
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;

  public MarkdownPdfRenderer() {
    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode(TemplateMode.TEXT);
    resolver.setCacheable(false);
    this.thymeleaf = new TemplateEngine();
    this.thymeleaf.setTemplateResolver(resolver);

    List<Extension> extensoes = List.of(TablesExtension.create());
    this.markdownParser = Parser.builder().extensions(extensoes).build();
    this.htmlRenderer = HtmlRenderer.builder().extensions(extensoes).build();
  }

  /**
   * Renderiza um template Markdown (com placeholders Thymeleaf) em PDF.
   *
   * @param markdownTemplate texto Markdown com expressões Thymeleaf (ex.: {@code [(${cliente})]})
   * @param contexto         variáveis disponíveis no template
   * @return bytes do PDF gerado
   */
  public byte[] renderToPdf(String markdownTemplate, Map<String, Object> contexto) {
    String markdown = aplicarTemplate(markdownTemplate, contexto);
    String corpoHtml = markdownToHtml(markdown);
    return htmlToPdf(envelopar(corpoHtml));
  }

  /**
   * Variante que devolve só o HTML — útil para preview no frontend antes
   * de gerar o PDF final.
   */
  public String renderToHtml(String markdownTemplate, Map<String, Object> contexto) {
    String markdown = aplicarTemplate(markdownTemplate, contexto);
    return envelopar(markdownToHtml(markdown));
  }

  private String aplicarTemplate(String template, Map<String, Object> contexto) {
    if (template == null || template.isBlank()) {
      return "";
    }
    Context ctx = new Context();
    if (contexto != null) {
      contexto.forEach(ctx::setVariable);
    }
    return thymeleaf.process(template, ctx);
  }

  private String markdownToHtml(String markdown) {
    return htmlRenderer.render(markdownParser.parse(markdown));
  }

  private String envelopar(String corpo) {
    return "<!DOCTYPE html>"
        + "<html><head><meta charset=\"UTF-8\"/>"
        + "<style>"
        + "body { font-family: Arial, sans-serif; font-size: 12px; margin: 24px; color: #1f2937; }"
        + "h1 { font-size: 22px; margin-top: 0; color: #111827; }"
        + "h2 { font-size: 16px; margin-top: 18px; color: #1f2937; border-bottom: 1px solid #e5e7eb; padding-bottom: 4px; }"
        + "h3 { font-size: 13px; margin-top: 12px; }"
        + "p, li { line-height: 1.45; }"
        + "ul, ol { padding-left: 18px; }"
        + "code { background: #f3f4f6; padding: 1px 4px; border-radius: 3px; font-family: 'Courier New', monospace; }"
        + "pre { background: #f3f4f6; padding: 8px; border-radius: 4px; overflow-x: auto; }"
        + "table { border-collapse: collapse; width: 100%; margin: 8px 0; }"
        + "th, td { border: 1px solid #d1d5db; padding: 4px 8px; text-align: left; }"
        + "th { background: #f9fafb; }"
        + "</style></head><body>"
        + corpo
        + "</body></html>";
  }

  private byte[] htmlToPdf(String html) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.toStream(baos);
      builder.run();
      return baos.toByteArray();
    } catch (Exception ex) {
      throw new BusinessException("Falha ao gerar PDF: " + ex.getMessage());
    }
  }
}
