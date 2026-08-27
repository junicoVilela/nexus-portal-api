package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.config.PublicacaoProperties;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.nexus.portal.shared.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconcilia publicações que ficaram presas em {@code GERANDO}. Sem isto, uma
 * queda do processo no meio da geração deixa a publicação em um estado que a UI
 * não consegue reprocessar nem excluir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicacaoWatchdogJob {

  private final PublicacaoRepository publicacaoRepository;
  private final PublicacaoEventService publicacaoEventService;
  private final PublicacaoProperties properties;
  private final StorageProperties storageProperties;

  @Scheduled(cron = "${docflow.publicacao.watchdog-cron:0 */5 * * * *}")
  @Transactional
  public void reconciliar() {
    OffsetDateTime limite = OffsetDateTime.now().minusMinutes(properties.timeoutMinutos());
    List<Publicacao> presas =
        publicacaoRepository.findByStatusAndUpdatedAtBefore(StatusPublicacao.GERANDO, limite);
    for (Publicacao publicacao : presas) {
      publicacao.registrarErro("Geração interrompida: sem conclusão em "
          + properties.timeoutMinutos() + " minutos. Reprocesse a publicação.");
      publicacaoEventService.publicar(publicacao);
      log.warn("Publicação {} estava em GERANDO desde {} e foi marcada como ERRO",
          publicacao.getId(), publicacao.getUpdatedAt());
    }
    removerDiretoriosDeGeracaoAbandonados(limite);
  }

  /**
   * A geração só apaga o diretório de trabalho no caminho feliz. Uma queda no
   * meio deixa {@code publicacoes/tmp/<geracaoId>} para trás — com as imagens
   * copiadas dentro, o que cresce rápido.
   */
  private void removerDiretoriosDeGeracaoAbandonados(OffsetDateTime limite) {
    Path tmp = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize().resolve("tmp");
    if (!Files.isDirectory(tmp)) {
      return;
    }
    try (Stream<Path> diretorios = Files.list(tmp)) {
      diretorios.filter(Files::isDirectory)
          .filter(dir -> modificadoAntes(dir, limite))
          .forEach(this::remover);
    } catch (IOException ex) {
      log.warn("Não foi possível varrer {} em busca de gerações abandonadas: {}", tmp, ex.getMessage());
    }
  }

  private boolean modificadoAntes(Path dir, OffsetDateTime limite) {
    try {
      Instant modificado = Files.getLastModifiedTime(dir).toInstant();
      return modificado.isBefore(limite.toInstant());
    } catch (IOException ex) {
      return false;
    }
  }

  private void remover(Path dir) {
    if (org.springframework.util.FileSystemUtils.deleteRecursively(dir.toFile())) {
      log.info("Diretório de geração abandonado removido: {}", dir);
    } else {
      log.warn("Diretório de geração abandonado não pôde ser removido: {}", dir);
    }
  }
}
