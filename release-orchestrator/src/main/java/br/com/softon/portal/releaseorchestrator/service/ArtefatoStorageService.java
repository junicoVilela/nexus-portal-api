package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.shared.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Storage filesystem dos artefatos de release (F0.5). Calcula SHA-256 em
 * single-pass enquanto grava em disco. Layout no diretório raiz:
 *
 * <pre>
 * {artefatosDir}/{releaseId}/{moduloProdutoId}/{uuid}-{nomeArquivo}
 * </pre>
 */
@RequiredArgsConstructor
@Service
public class ArtefatoStorageService {

  private final ReleaseOrchestratorStorageProperties properties;

  /**
   * Grava o conteúdo em disco e devolve metadata pra persistir.
   *
   * @param releaseId         release a que pertence
   * @param moduloProdutoId   módulo a que pertence
   * @param nomeArquivo       nome original do upload (sanitizado)
   * @param input             stream do conteúdo
   * @return path absoluto + SHA-256 hex + tamanho em bytes
   */
  public ArtefatoStored armazenar(UUID releaseId, UUID moduloProdutoId,
      String nomeArquivo, InputStream input) {
    Path destino = resolverDestino(releaseId, moduloProdutoId, nomeArquivo);
    try {
      Files.createDirectories(destino.getParent());
    } catch (IOException ex) {
      throw new BusinessException("Falha ao criar diretório de artefatos: " + ex.getMessage());
    }

    MessageDigest sha256 = sha256Digest();
    long tamanhoBytes;
    try (DigestInputStream digestStream = new DigestInputStream(input, sha256);
        OutputStream out = Files.newOutputStream(destino)) {
      tamanhoBytes = digestStream.transferTo(out);
    } catch (IOException ex) {
      tentarRemover(destino);
      throw new BusinessException("Falha ao gravar artefato: " + ex.getMessage());
    }

    return new ArtefatoStored(destino.toString(), HexFormat.of().formatHex(sha256.digest()), tamanhoBytes);
  }

  public Resource arquivo(String caminhoArmazenado) {
    Path path = Path.of(caminhoArmazenado);
    if (!Files.exists(path)) {
      throw new BusinessException("Arquivo do artefato não encontrado em disco: " + caminhoArmazenado);
    }
    return new PathResource(path);
  }

  public void remover(String caminhoArmazenado) {
    try {
      Files.deleteIfExists(Path.of(caminhoArmazenado));
    } catch (IOException ignored) {
      // remoção de melhor esforço; orphan files são limpos por housekeeping job
    }
  }

  private Path resolverDestino(UUID releaseId, UUID moduloProdutoId, String nomeArquivo) {
    String raiz = properties.artefatosDir();
    if (raiz == null || raiz.isBlank()) {
      throw new BusinessException("Configuração `release-orchestrator.storage.artefatos-dir` ausente.");
    }
    String nomeSanitizado = sanitizar(nomeArquivo);
    String filename = UUID.randomUUID() + "-" + nomeSanitizado;
    return Path.of(raiz, releaseId.toString(), moduloProdutoId.toString(), filename).normalize();
  }

  private String sanitizar(String nome) {
    if (nome == null || nome.isBlank()) {
      return "artefato";
    }
    String semPath = nome.replace('\\', '/').replaceAll(".*/", "");
    return semPath.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", ex);
    }
  }

  private void tentarRemover(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // ignora — falha de cleanup não deve mascarar erro original
    }
  }

  public record ArtefatoStored(String caminho, String sha256, long tamanhoBytes) {}
}
