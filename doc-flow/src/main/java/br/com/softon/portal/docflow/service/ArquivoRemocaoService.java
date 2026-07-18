package br.com.softon.portal.docflow.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class ArquivoRemocaoService {

  public void removerAposCommit(Path arquivo) {
    removerAposCommit(arquivo == null ? List.of() : List.of(arquivo));
  }

  public void removerAposCommit(Collection<Path> arquivos) {
    var caminhos = arquivos.stream().filter(Objects::nonNull).distinct().toList();
    if (caminhos.isEmpty()) return;

    Runnable remover = () -> caminhos.forEach(this::remover);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          remover.run();
        }
      });
      return;
    }
    remover.run();
  }

  private void remover(Path arquivo) {
    try {
      Files.deleteIfExists(arquivo);
    } catch (IOException | SecurityException exception) {
      log.warn("Não foi possível remover o arquivo em {}.", arquivo, exception);
    }
  }
}
