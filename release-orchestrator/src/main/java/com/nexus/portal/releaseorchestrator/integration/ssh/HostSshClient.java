package com.nexus.portal.releaseorchestrator.integration.ssh;

import com.nexus.portal.releaseorchestrator.entity.Host;
import java.time.Duration;

/** Execução remota no host Linux (JSch). Mesmo canal que o Jenkins usaria depois. */
public interface HostSshClient {

  boolean existe(Host host, String caminhoRemoto);

  void enviar(Host host, String caminhoRemoto, byte[] conteudo);

  Resultado exec(Host host, String comando, Duration timeout);

  record Resultado(int codigo, String stdout, String stderr) {
    public boolean ok() {
      return codigo == 0;
    }
  }
}
