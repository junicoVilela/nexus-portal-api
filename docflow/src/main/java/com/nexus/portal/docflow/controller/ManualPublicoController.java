package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.service.ManualCorpusService;
import com.nexus.portal.docflow.service.ManualCorpusService.Documento;
import com.nexus.portal.docflow.service.ManualLeitorService;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual vigente para os sistemas do cliente (Onda D, INT-401/402/404). Público no
 * {@code SecurityConfig}: a chave de integração (ou o link de prévia) no caminho identifica o
 * cliente — o navegador precisa navegar sem cabeçalho.
 *
 * <p>O "site" é o próprio ZIP publicado servido arquivo a arquivo: links relativos, busca e
 * {@code ?tela=} funcionam como no pacote, sem CDN.
 */
@RestController
@RequestMapping("/api/v1/manual")
public class ManualPublicoController {

  private static final String HELP_BRIDGE = carregarHelpBridge();

  private final ManualLeitorService leitorService;
  private final ManualCorpusService corpusService;

  public ManualPublicoController(ManualLeitorService leitorService, ManualCorpusService corpusService) {
    this.leitorService = leitorService;
    this.corpusService = corpusService;
  }

  public record Vigente(String cliente, String versao, UUID publicacaoId, OffsetDateTime publicadaEm,
      int quantidadePaginas, String site) {}

  /** {@code url} é relativa a {@code /api/v1/manual/{token}/}. */
  public record Tela(String codigoTela, String titulo, String caminho, String versao, String url) {}

  @GetMapping(value = "/help-bridge.js", produces = "text/javascript")
  public ResponseEntity<String> helpBridge() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
        .contentType(MediaType.parseMediaType("text/javascript;charset=UTF-8"))
        .body(HELP_BRIDGE);
  }

  @GetMapping("/{token}/vigente")
  public Vigente vigente(@PathVariable String token, @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origem) {
    var vigente = corpusService.vigente(leitorService.cliente(token, origem));
    return new Vigente(vigente.cliente(), vigente.versao(), vigente.publicacaoId(), vigente.publicadaEm(),
        vigente.quantidadePaginas(), "site/index.html");
  }

  /** INT-402/406: tela fora do manual é 404 de negócio, com mensagem — nunca 500. */
  @GetMapping("/{token}/tela/{codigoTela}")
  public Tela tela(@PathVariable String token, @PathVariable String codigoTela,
      @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origem) {
    var corpus = leitorService.corpusVigente(token, origem);
    Documento doc = corpus.documentos().entrySet().stream()
        .filter(e -> e.getKey().equalsIgnoreCase(codigoTela))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow(() -> new NotFoundException(
            "A tela " + codigoTela + " não está no manual vigente (" + corpus.rotulo() + ")."));
    String url = doc.url() == null ? "site/index.html?tela=" + doc.codigoTela() : "site/" + doc.url();
    return new Tela(doc.codigoTela(), doc.titulo(), doc.caminho(), corpus.versao(), url);
  }

  @GetMapping("/{token}/site")
  public ResponseEntity<Void> raiz(@PathVariable String token) {
    // Relativo a ".../{token}/site": cai em ".../{token}/site/index.html".
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("site/index.html")).build();
  }

  @GetMapping("/{token}/site/**")
  public ResponseEntity<byte[]> site(@PathVariable String token, HttpServletRequest request) {
    String prefixo = "/api/v1/manual/" + token + "/site/";
    String uri = request.getRequestURI();
    int inicio = uri.indexOf(prefixo);
    String caminho = inicio < 0 ? "" : uri.substring(inicio + prefixo.length());
    caminho = URLDecoder.decode(caminho, StandardCharsets.UTF_8);
    if (caminho.isEmpty()) {
      caminho = "index.html";
    }
    if (caminho.contains("..") || caminho.startsWith("/") || caminho.contains("\\")) {
      throw new NotFoundException("Arquivo não encontrado no manual.");
    }
    Path pacote = corpusService.vigente(leitorService.cliente(token, null)).zip();
    try (ZipFile zip = new ZipFile(pacote.toFile())) {
      ZipEntry entrada = zip.getEntry(caminho);
      if (entrada == null || entrada.isDirectory()) {
        throw new NotFoundException("Arquivo não encontrado no manual: " + caminho);
      }
      byte[] bytes;
      try (InputStream in = zip.getInputStream(entrada)) {
        bytes = in.readAllBytes();
      }
      MediaType tipo = caminho.endsWith(".webmanifest")
          ? MediaType.parseMediaType("application/manifest+json")
          : MediaTypeFactory.getMediaType(caminho).orElse(MediaType.APPLICATION_OCTET_STREAM);
      if (tipo.getType().equals("text") || tipo.getSubtype().contains("javascript") || tipo.getSubtype().endsWith("json")) {
        tipo = new MediaType(tipo, StandardCharsets.UTF_8);
      }
      return ResponseEntity.ok()
          .contentType(tipo)
          // O manual vigente muda quando sai nova publicação: cache curto.
          .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
          .body(bytes);
    } catch (IOException ex) {
      throw new UncheckedIOException("Falha ao ler o pacote do manual.", ex);
    }
  }

  private static String carregarHelpBridge() {
    try (InputStream in = ManualPublicoController.class.getResourceAsStream("/docflow/help-bridge.js")) {
      if (in == null) {
        throw new IllegalStateException("docflow/help-bridge.js ausente.");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
