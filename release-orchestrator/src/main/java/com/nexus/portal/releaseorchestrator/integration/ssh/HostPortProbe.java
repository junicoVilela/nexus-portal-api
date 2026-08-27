package com.nexus.portal.releaseorchestrator.integration.ssh;

import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consulta portas TCP em LISTEN no host ({@code ss}/{@code netstat}) para não
 * sugerir porta já em uso de verdade, além das reservas do inventário.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HostPortProbe {

  static final String COMANDO = "ss -H -tln 2>/dev/null | awk '{print $4}'; "
      + "netstat -lnt 2>/dev/null | awk 'NR>2 {print $4}'";

  private final HostSshClient ssh;
  private final LocalHostFsClient localFs;

  public Set<Integer> emUso(Host host) {
    if (host == null) {
      return emUsoLocal();
    }
    try {
      HostSshClient acesso = isLocal(host) ? localFs : ssh;
      HostSshClient.Resultado r = acesso.exec(host, COMANDO, Duration.ofSeconds(8));
      return parse(r.stdout());
    } catch (RuntimeException ex) {
      log.warn("Não consultou portas em uso em {}: {}", host.getHostname(), ex.getMessage());
      return Set.of();
    }
  }

  public Set<Integer> emUsoLocal() {
    Host local = new Host("LOCAL", "Esta máquina", "localhost",
        SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    return emUso(local);
  }

  static Set<Integer> parse(String stdout) {
    Set<Integer> portas = new LinkedHashSet<>();
    if (stdout == null || stdout.isBlank()) {
      return portas;
    }
    for (String line : stdout.split("\\R")) {
      String t = line.trim();
      if (t.isEmpty()) {
        continue;
      }
      int colon = t.lastIndexOf(':');
      if (colon < 0 || colon == t.length() - 1) {
        continue;
      }
      String num = t.substring(colon + 1).replaceAll("[^0-9]", "");
      if (num.isEmpty()) {
        continue;
      }
      try {
        int porta = Integer.parseInt(num);
        if (porta >= 1 && porta <= 65535) {
          portas.add(porta);
        }
      } catch (NumberFormatException ignored) {
        // linha sem porta
      }
    }
    return portas;
  }

  static boolean isLocal(Host host) {
    String h = host.getHostname() == null ? "" : host.getHostname().trim().toLowerCase(Locale.ROOT);
    return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1");
  }
}
