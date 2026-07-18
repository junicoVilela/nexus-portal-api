package br.com.softon.portal.docflow.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Service;

@Service
public class GeradorPdfService {

  public byte[] gerarPdf(String html) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      String htmlNormalizado = normalizarHtml(html);
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(htmlNormalizado, null);
      builder.toStream(baos);
      builder.run();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Erro ao gerar PDF: " + e.getMessage(), e);
    }
  }

  private String normalizarHtml(String html) {
    String entrada = html;
    if (entrada == null || entrada.isBlank()) entrada = "<html><body></body></html>";
    if (!entrada.toLowerCase().contains("<html")) {
      entrada = "<html><head><meta charset=\"UTF-8\"/>"
          + "<style>body{font-family:Arial,sans-serif;font-size:12px;margin:20px}"
          + "h1{font-size:20px}h2{font-size:16px}table{border-collapse:collapse;width:100%}"
          + "td,th{border:1px solid #ccc;padding:4px}</style>"
          + "</head><body>" + entrada + "</body></html>";
    }
    Document documento = Jsoup.parse(entrada);
    documento.childNodes().stream()
        .filter(DocumentType.class::isInstance)
        .toList()
        .forEach(org.jsoup.nodes.Node::remove);
    documento.outputSettings()
        .syntax(Document.OutputSettings.Syntax.xml)
        .escapeMode(Entities.EscapeMode.xhtml)
        .charset(StandardCharsets.UTF_8)
        .prettyPrint(false);
    return documento.outerHtml();
  }
}
