package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeradorPdfServiceTest {

  private final GeradorPdfService service = new GeradorPdfService();

  @Test
  void geraPdfAPartirDeDocumentoHtml5ComDoctype() {
    byte[] pdf = service.gerarPdf("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
        + "</head><body><h1>Manual</h1><p>Conteúdo de publicação.</p></body></html>");

    assertThat(pdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
    assertThat(pdf.length).isGreaterThan(500);
  }
}
