package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiTipoDocumento;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class AiDocumentoExtratorService {

  static final long TAMANHO_MAXIMO_BYTES = 15L * 1024 * 1024;
  static final int CARACTERES_MAXIMOS = 500_000;
  private static final int PAGINAS_PDF_MAXIMAS = 300;
  private static final int DOCX_XML_MAXIMO_BYTES = 20 * 1024 * 1024;
  private static final int DOCX_XML_TOTAL_MAXIMO_BYTES = 25 * 1024 * 1024;
  private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
  private static final Set<String> EXTENSOES = Set.of("doc", "docx", "pdf", "txt");

  public DocumentoExtraido extrair(MultipartFile arquivo) {
    validarArquivo(arquivo);
    String nome = sanitizarNome(arquivo.getOriginalFilename());
    String extensao = extensao(nome);
    byte[] bytes = lerBytes(arquivo);
    AiTipoDocumento tipo = tipo(extensao);
    validarAssinatura(tipo, bytes);

    try {
      return switch (tipo) {
        case DOC -> extrairDoc(nome, bytes);
        case DOCX -> extrairDocx(nome, bytes);
        case PDF -> extrairPdf(nome, bytes);
        case TXT -> extrairTxt(nome, bytes);
      };
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("Não foi possível interpretar o arquivo. Verifique se ele não está corrompido.");
    }
  }

  private DocumentoExtraido extrairDoc(String nome, byte[] bytes) throws IOException {
    try (var document = new HWPFDocument(new ByteArrayInputStream(bytes));
        var extractor = new WordExtractor(document)) {
      String texto = normalizar(String.join("\n\n", extractor.getParagraphText()));
      validarTextoExtraido(texto, "O DOC não possui texto suficiente para montar páginas.");
      return new DocumentoExtraido(
          nome,
          AiTipoDocumento.DOC,
          texto,
          1,
          List.of("A hierarquia do DOC legado foi inferida pelo texto; revise a ordem antes de gerar."));
    }
  }

  private DocumentoExtraido extrairDocx(String nome, byte[] bytes) throws Exception {
    DocxPartes partes = lerPartesDocx(bytes);
    var document = lerXmlSeguro(partes.documentoXml());
    Map<String, EstiloParagrafo> estilos = lerEstilos(partes.estilosXml());
    NodeList bodies = document.getElementsByTagNameNS(WORD_NS, "body");
    if (bodies.getLength() == 0) {
      throw new BusinessException("O DOCX não possui conteúdo reconhecível.");
    }

    var markdown = new StringBuilder();
    int numeroImagem = 0;
    NodeList filhos = bodies.item(0).getChildNodes();
    for (int i = 0; i < filhos.getLength(); i++) {
      Node filho = filhos.item(i);
      if (filho.getNodeType() != Node.ELEMENT_NODE) continue;
      if ("p".equals(filho.getLocalName())) {
        numeroImagem = adicionarParagrafo(markdown, (Element) filho, estilos, numeroImagem);
      } else if ("tbl".equals(filho.getLocalName())) {
        adicionarTabela(markdown, (Element) filho);
        numeroImagem = adicionarMarcadoresImagem(markdown, (Element) filho, numeroImagem);
      }
    }
    String texto = normalizar(markdown.toString());
    validarTextoExtraido(texto, "O DOCX não possui texto suficiente para montar páginas.");
    int totalPaginas = lerTotalPaginas(partes.aplicacaoXml(), document);
    List<String> avisos = new ArrayList<>();
    avisos.add("Títulos, listas e tabelas do DOCX foram preservados conforme os estilos do Word.");
    if (numeroImagem > 0) {
      avisos.add(numeroImagem
          + " referência(s) visual(is) foram identificadas e marcadas na posição original; "
          + "revise as referências visuais antes de publicar.");
    }
    return new DocumentoExtraido(
        nome,
        AiTipoDocumento.DOCX,
        texto,
        totalPaginas,
        List.copyOf(avisos));
  }

  private DocumentoExtraido extrairPdf(String nome, byte[] bytes) throws IOException {
    try (PDDocument document = PDDocument.load(bytes)) {
      if (document.isEncrypted()) {
        throw new BusinessException("O PDF está protegido por senha. Remova a proteção antes de importar.");
      }
      int paginas = document.getNumberOfPages();
      if (paginas > PAGINAS_PDF_MAXIMAS) {
        throw new BusinessException("O PDF possui mais de 300 páginas. Divida-o em arquivos menores.");
      }
      var stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      var texto = new StringBuilder();
      for (int pagina = 1; pagina <= paginas; pagina++) {
        stripper.setStartPage(pagina);
        stripper.setEndPage(pagina);
        String conteudo = stripper.getText(document).trim();
        if (!conteudo.isBlank()) {
          texto.append("\n\n").append(conteudo);
        }
        if (texto.length() > CARACTERES_MAXIMOS) {
          throw new BusinessException("O texto extraído excede 500.000 caracteres. Divida o manual em partes.");
        }
      }
      String resultado = normalizar(texto.toString());
      validarTextoExtraido(
          resultado,
          "O PDF não contém texto selecionável. Aplique OCR ou exporte-o como PDF pesquisável antes de importar.");
      return new DocumentoExtraido(
          nome,
          AiTipoDocumento.PDF,
          resultado,
          paginas,
          List.of("A hierarquia do PDF foi inferida pelo texto; revise a ordem das páginas antes de gerar."));
    } catch (InvalidPasswordException ex) {
      throw new BusinessException("O PDF está protegido por senha. Remova a proteção antes de importar.");
    }
  }

  private DocumentoExtraido extrairTxt(String nome, byte[] bytes) {
    String texto;
    try {
      var decoder = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);
      texto = decoder.decode(ByteBuffer.wrap(removerBom(bytes))).toString();
    } catch (CharacterCodingException ex) {
      throw new BusinessException("O TXT deve estar codificado em UTF-8.");
    }
    texto = normalizar(texto);
    validarTextoExtraido(texto, "O TXT não possui texto suficiente para montar páginas.");
    return new DocumentoExtraido(nome, AiTipoDocumento.TXT, texto, 1, List.of());
  }

  private void validarArquivo(MultipartFile arquivo) {
    if (arquivo == null || arquivo.isEmpty()) {
      throw new BusinessException("Selecione um arquivo DOC, DOCX, PDF ou TXT para importar.");
    }
    if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
      throw new BusinessException("O arquivo excede o limite de 15 MB.");
    }
    String ext = extensao(sanitizarNome(arquivo.getOriginalFilename()));
    if (!EXTENSOES.contains(ext)) {
      throw new BusinessException("Formato não suportado. Use DOC, DOCX, PDF pesquisável ou TXT em UTF-8.");
    }
  }

  private byte[] lerBytes(MultipartFile arquivo) {
    try {
      return arquivo.getBytes();
    } catch (IOException ex) {
      throw new BusinessException("Não foi possível ler o arquivo enviado.");
    }
  }

  private DocxPartes lerPartesDocx(byte[] bytes) throws IOException {
    byte[] documentoXml = null;
    byte[] estilosXml = null;
    byte[] aplicacaoXml = null;
    int totalXml = 0;
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      int entradas = 0;
      while ((entry = zip.getNextEntry()) != null) {
        entradas++;
        if (entradas > 500) {
          throw new BusinessException("O DOCX possui uma estrutura interna acima do limite permitido.");
        }
        String caminho = entry.getName();
        if (!Set.of("word/document.xml", "word/styles.xml", "docProps/app.xml").contains(caminho)) continue;
        byte[] conteudo = lerEntradaDocx(zip);
        totalXml += conteudo.length;
        if (totalXml > DOCX_XML_TOTAL_MAXIMO_BYTES) {
          throw new BusinessException("O conteúdo interno do DOCX excede o limite permitido.");
        }
        if ("word/document.xml".equals(caminho)) documentoXml = conteudo;
        else if ("word/styles.xml".equals(caminho)) estilosXml = conteudo;
        else aplicacaoXml = conteudo;
      }
    }
    if (documentoXml == null) throw new BusinessException("O arquivo não é um DOCX válido.");
    return new DocxPartes(documentoXml, estilosXml, aplicacaoXml);
  }

  private byte[] lerEntradaDocx(ZipInputStream zip) throws IOException {
    var saida = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int total = 0;
    int lidos;
    while ((lidos = zip.read(buffer)) != -1) {
      total += lidos;
      if (total > DOCX_XML_MAXIMO_BYTES) {
        throw new BusinessException("O conteúdo interno do DOCX excede o limite permitido.");
      }
      saida.write(buffer, 0, lidos);
    }
    return saida.toByteArray();
  }

  private org.w3c.dom.Document lerXmlSeguro(byte[] xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
  }

  private Map<String, EstiloParagrafo> lerEstilos(byte[] estilosXml) throws Exception {
    if (estilosXml == null || estilosXml.length == 0) return Map.of();
    var document = lerXmlSeguro(estilosXml);
    NodeList estilos = document.getElementsByTagNameNS(WORD_NS, "style");
    Map<String, EstiloParagrafo> resultado = new HashMap<>();
    for (int i = 0; i < estilos.getLength(); i++) {
      Element estilo = (Element) estilos.item(i);
      String tipo = atributoWord(estilo, "type");
      if (!tipo.isBlank() && !"paragraph".equalsIgnoreCase(tipo)) continue;
      String id = atributoWord(estilo, "styleId");
      if (id.isBlank()) continue;
      String nome = valorPrimeiroFilho(estilo, "name");
      String identificacao = normalizarIdentificador(id + " " + nome);
      boolean sumario = identificacao.matches(".*(toc|sumario)[0-9]*.*");
      int nivel = nivelPeloEstilo(estilo, identificacao);
      resultado.put(id, new EstiloParagrafo(nivel, sumario));
    }
    return Map.copyOf(resultado);
  }

  private int nivelPeloEstilo(Element estilo, String identificacao) {
    NodeList niveis = estilo.getElementsByTagNameNS(WORD_NS, "outlineLvl");
    if (niveis.getLength() > 0) {
      String valor = atributoWord((Element) niveis.item(0), "val");
      try {
        int nivel = Integer.parseInt(valor) + 1;
        if (nivel >= 1 && nivel <= 6) return nivel;
      } catch (NumberFormatException ignored) {
        // Usa o nome do estilo como fallback.
      }
    }
    var matcher = java.util.regex.Pattern
        .compile(".*(?:heading|titulo|ttulo)\\s*([1-6]).*")
        .matcher(identificacao);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private int adicionarParagrafo(
      StringBuilder markdown,
      Element paragrafo,
      Map<String, EstiloParagrafo> estilos,
      int numeroImagem) {
    EstiloParagrafo estilo = estiloParagrafo(paragrafo, estilos);
    if (estilo.sumario()) return numeroImagem;
    String texto = textoElemento(paragrafo).trim();
    int nivel = ajustarNivelPelaNumeracao(estilo.nivelTitulo(), texto);
    if (nivel > 0) {
      if (!texto.isBlank()) {
        markdown.append("\n").append("#".repeat(nivel)).append(' ').append(texto).append("\n");
      }
      return adicionarMarcadoresImagem(markdown, paragrafo, numeroImagem);
    }
    if (!texto.isBlank()) {
      boolean lista = paragrafo.getElementsByTagNameNS(WORD_NS, "numPr").getLength() > 0;
      markdown.append(lista ? "- " : "").append(texto).append("\n\n");
    }
    return adicionarMarcadoresImagem(markdown, paragrafo, numeroImagem);
  }

  private int ajustarNivelPelaNumeracao(int nivelEstilo, String texto) {
    if (nivelEstilo <= 0 || texto == null || texto.isBlank()) return nivelEstilo;
    var matcher = java.util.regex.Pattern
        .compile("^(\\d+(?:\\.\\d+){0,5})[.)]?\\s+.*$")
        .matcher(texto);
    if (!matcher.matches()) return nivelEstilo;
    int nivelNumeracao = matcher.group(1).split("\\.").length;
    return Math.min(6, Math.max(nivelEstilo, nivelNumeracao));
  }

  private void adicionarTabela(StringBuilder markdown, Element tabela) {
    NodeList linhas = tabela.getElementsByTagNameNS(WORD_NS, "tr");
    for (int i = 0; i < linhas.getLength(); i++) {
      Element linha = (Element) linhas.item(i);
      NodeList celulas = linha.getElementsByTagNameNS(WORD_NS, "tc");
      var valores = new ArrayList<String>();
      for (int j = 0; j < celulas.getLength(); j++) {
        valores.add(textoElemento((Element) celulas.item(j)).trim().replace("|", "\\|"));
      }
      markdown.append("| ").append(String.join(" | ", valores)).append(" |\n");
      if (i == 0 && !valores.isEmpty()) {
        markdown.append("| ").append("--- | ".repeat(valores.size())).append("\n");
      }
    }
    markdown.append("\n");
  }

  private EstiloParagrafo estiloParagrafo(
      Element paragrafo, Map<String, EstiloParagrafo> estilosConhecidos) {
    NodeList estilos = paragrafo.getElementsByTagNameNS(WORD_NS, "pStyle");
    if (estilos.getLength() == 0) return EstiloParagrafo.PADRAO;
    Element estilo = (Element) estilos.item(0);
    String valor = atributoWord(estilo, "val");
    EstiloParagrafo conhecido = estilosConhecidos.get(valor);
    if (conhecido != null) return conhecido;
    String normalizado = normalizarIdentificador(valor);
    boolean sumario = normalizado.matches(".*(toc|sumario)[0-9]*.*");
    var matcher = java.util.regex.Pattern
        .compile(".*(?:heading|titulo|ttulo)\\s*([1-6]).*")
        .matcher(normalizado);
    int nivel = matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    return new EstiloParagrafo(nivel, sumario);
  }

  private String textoElemento(Element elemento) {
    NodeList textos = elemento.getElementsByTagNameNS(WORD_NS, "t");
    var resultado = new StringBuilder();
    for (int i = 0; i < textos.getLength(); i++) {
      resultado.append(textos.item(i).getTextContent());
    }
    return resultado.toString().replaceAll("[ \\t]+", " ");
  }

  private int adicionarMarcadoresImagem(
      StringBuilder markdown, Element elemento, int numeroImagem) {
    int imagens = elemento.getElementsByTagNameNS("*", "blip").getLength()
        + elemento.getElementsByTagNameNS("*", "imagedata").getLength();
    for (int i = 0; i < imagens; i++) {
      markdown.append("> [Imagem ").append(++numeroImagem)
          .append(" do documento original]\n\n");
    }
    return numeroImagem;
  }

  private int lerTotalPaginas(byte[] aplicacaoXml, org.w3c.dom.Document documento) {
    if (aplicacaoXml != null && aplicacaoXml.length > 0) {
      try {
        NodeList paginas = lerXmlSeguro(aplicacaoXml).getElementsByTagName("Pages");
        if (paginas.getLength() > 0) {
          int total = Integer.parseInt(paginas.item(0).getTextContent().trim());
          if (total > 0) return total;
        }
      } catch (Exception ignored) {
        // Metadado opcional; usa as quebras renderizadas como fallback.
      }
    }
    int quebrasRenderizadas = documento
        .getElementsByTagNameNS(WORD_NS, "lastRenderedPageBreak")
        .getLength();
    return Math.max(1, quebrasRenderizadas + 1);
  }

  private String valorPrimeiroFilho(Element elemento, String nomeLocal) {
    NodeList filhos = elemento.getElementsByTagNameNS(WORD_NS, nomeLocal);
    if (filhos.getLength() == 0) return "";
    return atributoWord((Element) filhos.item(0), "val");
  }

  private String atributoWord(Element elemento, String nome) {
    String valor = elemento.getAttributeNS(WORD_NS, nome);
    return valor.isBlank() ? elemento.getAttribute("w:" + nome) : valor;
  }

  private String normalizarIdentificador(String valor) {
    return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]", "");
  }

  private void validarAssinatura(AiTipoDocumento tipo, byte[] bytes) {
    boolean valida = switch (tipo) {
      case PDF -> bytes.length >= 5
          && bytes[0] == '%'
          && bytes[1] == 'P'
          && bytes[2] == 'D'
          && bytes[3] == 'F'
          && bytes[4] == '-';
      case DOCX -> bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K';
      case DOC -> bytes.length >= 8
          && (bytes[0] & 0xFF) == 0xD0
          && (bytes[1] & 0xFF) == 0xCF
          && (bytes[2] & 0xFF) == 0x11
          && (bytes[3] & 0xFF) == 0xE0
          && (bytes[4] & 0xFF) == 0xA1
          && (bytes[5] & 0xFF) == 0xB1
          && (bytes[6] & 0xFF) == 0x1A
          && (bytes[7] & 0xFF) == 0xE1;
      case TXT -> !contemByteNulo(bytes);
    };
    if (!valida) {
      throw new BusinessException("A extensão do arquivo não corresponde ao conteúdo enviado.");
    }
  }

  private boolean contemByteNulo(byte[] bytes) {
    int limite = Math.min(bytes.length, 4096);
    for (int i = 0; i < limite; i++) {
      if (bytes[i] == 0) return true;
    }
    return false;
  }

  private void validarTextoExtraido(String texto, String mensagemVazio) {
    if (texto.replaceAll("\\s", "").length() < 40) throw new BusinessException(mensagemVazio);
    if (texto.length() > CARACTERES_MAXIMOS) {
      throw new BusinessException("O texto extraído excede 500.000 caracteres. Divida o manual em partes.");
    }
  }

  private byte[] removerBom(byte[] bytes) {
    if (bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xEF
        && (bytes[1] & 0xFF) == 0xBB
        && (bytes[2] & 0xFF) == 0xBF) {
      return java.util.Arrays.copyOfRange(bytes, 3, bytes.length);
    }
    return bytes;
  }

  private String normalizar(String texto) {
    return texto
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replaceAll("[ \\t]+\\n", "\n")
        .replaceAll("\\n{4,}", "\n\n\n")
        .trim();
  }

  static String sanitizarNome(String original) {
    String nome = original == null || original.isBlank() ? "documento" : original.replace('\\', '/');
    nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
    return nome.isBlank() ? "documento" : nome.substring(0, Math.min(nome.length(), 255));
  }

  private String extensao(String nome) {
    int ponto = nome.lastIndexOf('.');
    return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
  }

  private AiTipoDocumento tipo(String extensao) {
    return AiTipoDocumento.valueOf(extensao.toUpperCase(Locale.ROOT));
  }

  public record DocumentoExtraido(
      String nomeArquivo,
      AiTipoDocumento tipo,
      String texto,
      int totalPaginasOrigem,
      List<String> avisos) {}

  private record DocxPartes(byte[] documentoXml, byte[] estilosXml, byte[] aplicacaoXml) {}

  private record EstiloParagrafo(int nivelTitulo, boolean sumario) {
    private static final EstiloParagrafo PADRAO = new EstiloParagrafo(0, false);
  }
}
