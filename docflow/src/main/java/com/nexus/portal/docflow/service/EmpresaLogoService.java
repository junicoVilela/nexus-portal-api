package com.nexus.portal.docflow.service;

import com.nexus.portal.shared.config.StorageProperties;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EmpresaLogoService {

  private static final String[] EXTS = {".png", ".jpg", ".jpeg", ".webp", ".svg", ".gif"};

  private final StorageProperties storageProperties;

  public void salvarLogo(MultipartFile file) {
    validar(file);
    String ext = extensao(file.getOriginalFilename(), file.getContentType());
    Path dir = empresaDir();
    Path destino = dir.resolve("logo" + ext);
    try {
      Files.createDirectories(dir);
      // remove logo anterior (qualquer extensão)
      for (String e : EXTS) {
        Files.deleteIfExists(dir.resolve("logo" + e));
      }
      file.transferTo(destino);
    } catch (IOException ex) {
      throw new BusinessException("Falha ao salvar logo da empresa: " + ex.getMessage());
    }
  }

  public ResponseEntity<Resource> servir() {
    Optional<Path> logo = encontrar();
    if (logo.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    Path path = logo.get();
    String contentType = detectarContentType(path.getFileName().toString());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .body(new PathResource(path));
  }

  public void removerLogo() {
    Path dir = empresaDir();
    for (String ext : EXTS) {
      try {
        Files.deleteIfExists(dir.resolve("logo" + ext));
      } catch (IOException ignored) {
        // continua removendo os demais
      }
    }
  }

  /** Retorna o path do logo da empresa se existir — usado pelo gerador de pacote. */
  public Optional<Path> encontrar() {
    Path dir = empresaDir();
    for (String ext : EXTS) {
      Path candidate = dir.resolve("logo" + ext);
      if (Files.exists(candidate)) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  public Path empresaDir() {
    Path base = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize();
    Path parent = base.getParent() != null ? base.getParent() : base;
    return parent.resolve("empresa");
  }

  private void validar(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("Selecione uma imagem para o logo da empresa.");
    }
    String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!ct.startsWith("image/")) {
      throw new BusinessException("Apenas imagens são aceitas como logo.");
    }
    if (file.getSize() > 4 * 1024 * 1024) {
      throw new BusinessException("Logo maior que 4 MB.");
    }
  }

  private String extensao(String nome, String contentType) {
    if (nome != null) {
      int dot = nome.lastIndexOf('.');
      if (dot >= 0 && dot < nome.length() - 1) {
        return nome.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase(Locale.ROOT);
      }
    }
    return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      case "image/svg+xml" -> ".svg";
      default -> ".jpg";
    };
  }

  private String detectarContentType(String filename) {
    if (filename.endsWith(".png")) return "image/png";
    if (filename.endsWith(".webp")) return "image/webp";
    if (filename.endsWith(".gif")) return "image/gif";
    if (filename.endsWith(".svg")) return "image/svg+xml";
    return "image/jpeg";
  }
}
