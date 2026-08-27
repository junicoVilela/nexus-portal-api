package com.nexus.portal.docflow.service;

import com.nexus.portal.shared.config.StorageProperties;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Layout dos anexos em disco. Fica separado do {@code PaginaAnexoService} para
 * que a duplicação de página possa copiar arquivos sem criar dependência
 * circular entre os serviços de página e de anexo.
 */
@Service
@RequiredArgsConstructor
class AnexoStorage {

  private final StorageProperties storageProperties;

  Path dirDaPagina(UUID paginaId) {
    return raiz().resolve(paginaId.toString());
  }

  Path novoArquivo(UUID paginaId, String nomeOriginal, String contentType) {
    return dirDaPagina(paginaId)
        .resolve(UUID.randomUUID() + extensao(nomeOriginal, contentType))
        .normalize();
  }

  private Path raiz() {
    Path publicacoesDir = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize();
    Path base = publicacoesDir.getParent() == null ? publicacoesDir : publicacoesDir.getParent();
    return base.resolve("anexos");
  }

  String extensao(String nomeOriginal, String contentType) {
    int dot = nomeOriginal == null ? -1 : nomeOriginal.lastIndexOf('.');
    if (dot >= 0 && dot < nomeOriginal.length() - 1) {
      return nomeOriginal.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase(Locale.ROOT);
    }
    return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      default -> ".jpg";
    };
  }
}
