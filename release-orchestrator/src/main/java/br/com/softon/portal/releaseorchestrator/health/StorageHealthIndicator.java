package br.com.softon.portal.releaseorchestrator.health;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health do storage local — verifica que {@code artefatos-dir} e
 * {@code entregas-dir} existem, são graváveis e têm pelo menos
 * {@value #ESPACO_MINIMO_BYTES} bytes livres. Decisão de deploy
 * (memória {@code storage-disco-local-servidor.md}): tudo em disco local
 * do servidor portal, sem NFS/S3.
 *
 * <p>Aparece em {@code /actuator/health} sob a chave {@code releaseOrchestratorStorage}.
 */
@Component("releaseOrchestratorStorageHealthIndicator")
@RequiredArgsConstructor
public class StorageHealthIndicator implements HealthIndicator {

  /** 1 GB — limite arbitrário pra alertar antes do disco encher. */
  private static final long ESPACO_MINIMO_BYTES = 1024L * 1024L * 1024L;

  private final ReleaseOrchestratorStorageProperties storage;

  @Override
  public Health health() {
    Health.Builder builder = Health.up();
    boolean degradado = false;

    degradado |= !checarPasta(builder, "artefatosDir", storage.artefatosDir());
    degradado |= !checarPasta(builder, "entregasDir", storage.entregasDir());

    return degradado ? builder.down().build() : builder.build();
  }

  /**
   * Verifica uma pasta de storage e popula detalhes no builder.
   *
   * @return true se a pasta está OK; false se falhou (caller marca DOWN).
   */
  private boolean checarPasta(Health.Builder builder, String chave, String caminho) {
    if (caminho == null || caminho.isBlank()) {
      builder.withDetail(chave, "não configurado");
      return false;
    }
    Path path = Path.of(caminho);
    try {
      Files.createDirectories(path);
      if (!Files.isWritable(path)) {
        builder.withDetail(chave, "não gravável: " + caminho);
        return false;
      }
      FileStore fs = Files.getFileStore(path);
      long livres = fs.getUsableSpace();
      long total = fs.getTotalSpace();
      builder.withDetail(chave, caminho)
          .withDetail(chave + ".espacoLivreMB", livres / (1024 * 1024))
          .withDetail(chave + ".espacoTotalMB", total / (1024 * 1024));
      if (livres < ESPACO_MINIMO_BYTES) {
        builder.withDetail(chave + ".alerta",
            "espaço livre < " + (ESPACO_MINIMO_BYTES / (1024 * 1024)) + " MB");
        return false;
      }
      return true;
    } catch (IOException e) {
      builder.withDetail(chave, "erro: " + e.getMessage());
      return false;
    }
  }
}
