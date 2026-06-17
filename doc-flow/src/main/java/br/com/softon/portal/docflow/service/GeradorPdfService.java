package br.com.softon.portal.docflow.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
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
    if (html == null) return "<html><body></body></html>";
    if (!html.trim().startsWith("<!")) {
      return "<!doctype html><html><head><meta charset=\"UTF-8\"/>"
          + "<style>body{font-family:Arial,sans-serif;font-size:12px;margin:20px}"
          + "h1{font-size:20px}h2{font-size:16px}table{border-collapse:collapse;width:100%}"
          + "td,th{border:1px solid #ccc;padding:4px}</style>"
          + "</head><body>" + html + "</body></html>";
    }
    return html;
  }
}
