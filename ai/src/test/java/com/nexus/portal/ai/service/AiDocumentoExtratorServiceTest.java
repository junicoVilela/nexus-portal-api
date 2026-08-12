package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.ai.entity.AiTipoDocumento;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AiDocumentoExtratorServiceTest {

  private final AiDocumentoExtratorService service = new AiDocumentoExtratorService();

  @Test
  void extrairTxt_preservaMarkdown() {
    String conteudo = """
        # Cadastro de produto

        ## Objetivo

        Permitir cadastrar e consultar produtos no sistema com segurança.
        """;
    var arquivo = new MockMultipartFile(
        "arquivo", "manual.txt", "text/plain", conteudo.getBytes(StandardCharsets.UTF_8));

    var extraido = service.extrair(arquivo);

    assertThat(extraido.tipo()).isEqualTo(AiTipoDocumento.TXT);
    assertThat(extraido.texto()).contains("# Cadastro de produto", "## Objetivo");
    assertThat(extraido.totalPaginasOrigem()).isEqualTo(1);
  }

  @Test
  void extrairDocx_converteTitulosListasETabelaParaMarkdown() throws Exception {
    String xml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:body>
            <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Manual de clientes</w:t></w:r></w:p>
            <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Cadastro</w:t></w:r></w:p>
            <w:p><w:r><w:t>Preencha os campos obrigatórios e clique em Salvar.</w:t></w:r></w:p>
            <w:tbl><w:tr><w:tc><w:p><w:r><w:t>Campo</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>Regra</w:t></w:r></w:p></w:tc></w:tr>
              <w:tr><w:tc><w:p><w:r><w:t>Nome</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>Obrigatório</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
          </w:body>
        </w:document>
        """;
    var arquivo = new MockMultipartFile(
        "arquivo",
        "manual.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        docx(xml));

    var extraido = service.extrair(arquivo);

    assertThat(extraido.tipo()).isEqualTo(AiTipoDocumento.DOCX);
    assertThat(extraido.texto())
        .contains("# Manual de clientes", "## Cadastro", "| Campo | Regra |", "| Nome | Obrigatório |");
  }

  @Test
  void extrairPdf_lêTextoSelecionavelEPreservaQuantidadeDePaginas() throws Exception {
    var arquivo = new MockMultipartFile("arquivo", "manual.pdf", "application/pdf", pdfComTexto());

    var extraido = service.extrair(arquivo);

    assertThat(extraido.tipo()).isEqualTo(AiTipoDocumento.PDF);
    assertThat(extraido.totalPaginasOrigem()).isEqualTo(2);
    assertThat(extraido.texto()).contains("Cadastro de cliente", "Consulta e atualizacao");
    assertThat(extraido.avisos()).anyMatch(aviso -> aviso.contains("hierarquia"));
  }

  @Test
  void extrairPdf_semTextoExplicaNecessidadeDeOcr() throws Exception {
    byte[] pdf;
    try (var document = new PDDocument()) {
      document.addPage(new PDPage());
      var saida = new ByteArrayOutputStream();
      document.save(saida);
      pdf = saida.toByteArray();
    }

    var arquivo = new MockMultipartFile("arquivo", "scan.pdf", "application/pdf", pdf);

    assertThatThrownBy(() -> service.extrair(arquivo))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("OCR");
  }

  @Test
  void extrairDoc_comAssinaturaInvalidaRecusaArquivoDisfarcado() {
    var arquivo = new MockMultipartFile(
        "arquivo", "manual.doc", "application/msword", "conteudo antigo".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.extrair(arquivo))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("extensão");
  }

  private byte[] docx(String documentXml) throws Exception {
    var saida = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(saida)) {
      zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
      zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("word/document.xml"));
      zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return saida.toByteArray();
  }

  private byte[] pdfComTexto() throws Exception {
    try (var document = new PDDocument()) {
      adicionarPagina(document, "1 Cadastro de cliente - preencha os campos obrigatorios e salve o registro.");
      adicionarPagina(document, "2 Consulta e atualizacao - pesquise, edite e confirme as informacoes apresentadas.");
      var saida = new ByteArrayOutputStream();
      document.save(saida);
      return saida.toByteArray();
    }
  }

  private void adicionarPagina(PDDocument document, String texto) throws Exception {
    var pagina = new PDPage();
    document.addPage(pagina);
    try (var conteudo = new PDPageContentStream(document, pagina)) {
      conteudo.beginText();
      conteudo.setFont(PDType1Font.HELVETICA, 12);
      conteudo.newLineAtOffset(40, 700);
      conteudo.showText(texto);
      conteudo.endText();
    }
  }
}
