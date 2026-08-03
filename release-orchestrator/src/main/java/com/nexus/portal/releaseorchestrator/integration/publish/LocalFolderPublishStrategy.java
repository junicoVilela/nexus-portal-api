package com.nexus.portal.releaseorchestrator.integration.publish;

import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publica o pacote em uma pasta local do servidor. Estratégia MVP usada
 * antes da F3; preserva o comportamento já em produção.
 *
 * <p>O ZIP é copiado de {@code pacote} para {@code config.caminhoBase}.
 * A pasta destino é criada se não existir. Se o arquivo já existir,
 * é sobrescrito (operações de geração são idempotentes).
 */
@Slf4j
@Component
public class LocalFolderPublishStrategy implements PublishStrategy {

  @Override
  public TipoDestinoEntrega tipo() {
    return TipoDestinoEntrega.PASTA;
  }

  @Override
  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    if (config.getCaminhoBase() == null || config.getCaminhoBase().isBlank()) {
      throw new PublishException("Caminho base não configurado para destino PASTA.");
    }
    Path destinoDir = Path.of(config.getCaminhoBase());
    try {
      Files.createDirectories(destinoDir);
      Path destinoFinal = destinoDir.resolve(pacote.getFileName());
      Files.copy(pacote, destinoFinal, StandardCopyOption.REPLACE_EXISTING);
      long tamanho = Files.size(destinoFinal);
      log.info("Pacote publicado em {} ({} bytes)", destinoFinal, tamanho);
      return new PublishResult(destinoFinal.toAbsolutePath().toString(), tamanho,
          OffsetDateTime.now());
    } catch (IOException e) {
      throw new PublishException("Falha ao copiar pacote para " + destinoDir, e);
    }
  }

  @Override
  public void testarConexao(ConfigEntrega config) {
    if (config.getCaminhoBase() == null || config.getCaminhoBase().isBlank()) {
      throw new PublishException("Caminho base não configurado.");
    }
    Path destinoDir = Path.of(config.getCaminhoBase());
    try {
      Files.createDirectories(destinoDir);
      if (!Files.isWritable(destinoDir)) {
        throw new PublishException("Pasta " + destinoDir + " sem permissão de escrita.");
      }
    } catch (IOException e) {
      throw new PublishException("Pasta inacessível: " + destinoDir + " — " + e.getMessage(), e);
    }
  }
}
