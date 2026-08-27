package com.nexus.portal.releaseorchestrator.integration.ssh;

import com.nexus.portal.releaseorchestrator.entity.Host;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Acesso ao filesystem local (lab em localhost, sem SSH). */
@Component
public class LocalHostFsClient implements HostSshClient {

  @Override
  public boolean existe(Host host, String caminhoRemoto) {
    return Files.exists(Path.of(caminhoRemoto));
  }

  @Override
  public void enviar(Host host, String caminhoRemoto, byte[] conteudo) {
    try {
      Path path = Path.of(caminhoRemoto);
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.write(path, conteudo);
    } catch (IOException e) {
      throw new SshHostException("Não gravou " + caminhoRemoto + ": " + e.getMessage(), e);
    }
  }

  @Override
  public Resultado exec(Host host, String comando, Duration timeout) {
    try {
      Process process = new ProcessBuilder("bash", "-c", comando)
          .redirectErrorStream(false)
          .start();
      boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new SshHostException("Timeout local (" + timeout.toSeconds() + "s) ao executar script.");
      }
      return new Resultado(process.exitValue(), ler(process.getInputStream()), ler(process.getErrorStream()));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SshHostException("Execução local interrompida.", e);
    } catch (IOException e) {
      throw new SshHostException("Falha ao executar localmente: " + e.getMessage(), e);
    }
  }

  public void copiarArvore(Path origem, Path destino) {
    LinuxManualTreeCopier.copiar(origem, destino);
  }

  private static String ler(InputStream in) throws IOException {
    try (in) {
      return new String(in.readAllBytes());
    }
  }
}
