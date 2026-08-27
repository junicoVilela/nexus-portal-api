package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.config.LinuxManualProperties;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.integration.ssh.HostSshClient;
import com.nexus.portal.releaseorchestrator.integration.ssh.JschHostSshClient;
import com.nexus.portal.releaseorchestrator.integration.ssh.LocalHostFsClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Linux manual: CRIAR copia o esqueleto (scripts/templates/artifacts) e roda
 * {@code install.sh}, que baixa JDK 8+17 e Tomcat. ATUALIZAR reescreve o
 * {@code .env} e chama o mesmo script (já pula download se o runtime existir).
 */
@Component
@RequiredArgsConstructor
public class LinuxManualImplantacaoAdapter implements ImplantacaoAdapter {

  static final Duration TIMEOUT_INSTALL = Duration.ofMinutes(20);
  static final Duration TIMEOUT_START = Duration.ofMinutes(5);
  static final Duration TIMEOUT_STOP = Duration.ofSeconds(60);

  private final HostSshClient ssh;
  private final LocalHostFsClient localFs;
  private final LinuxManualEnvFactory envFactory;
  private final LinuxManualProperties properties;

  @Override
  public Resultado aplicar(Contexto contexto) {
    if (contexto.instalacao().getTipoImplantacao() != TipoImplantacao.LINUX_MANUAL) {
      return Resultado.falha("Adaptador Linux manual chamado para tipo "
          + contexto.instalacao().getTipoImplantacao() + ".");
    }
    Host host = contexto.host();
    boolean local = isLocal(host);
    if (!local && host.getTipoConexao() != TipoConexaoHost.SSH) {
      return Resultado.falha("Linux manual em modo REAL exige host SSH (ou localhost para lab).");
    }
    String dir = primeiroNaoVazio(
        contexto.manifesto().diretorioInstalacao(),
        contexto.instalacao().getDiretorioInstalacao());
    if (dir == null) {
      return Resultado.falha("Informe o diretório de destino no host (ex.: /tmp/nexus-lab/bmw).");
    }
    if (!dir.matches("/[A-Za-z0-9._/-]+")) {
      return Resultado.falha("Diretório de instalação inválido (use caminho absoluto Unix sem espaços).");
    }
    HostSshClient acesso = escolher(host, local);
    if (contexto.operacao().cicloVida()) {
      try {
        return cicloVida(contexto, host, dir, acesso);
      } catch (RuntimeException ex) {
        return Resultado.falha(ex.getMessage());
      }
    }
    try {
      boolean criar = contexto.operacao() == OperacaoDeploy.CRIAR;
      if (!acesso.existe(host, dir + "/scripts/install.sh")) {
        if (!criar) {
          return Resultado.falha("Não achei scripts/install.sh em " + dir
              + ". Na primeira vez use Criar para materializar o instalador.");
        }
        if (!properties.temTemplate()) {
          return Resultado.falha("Não achei scripts/install.sh em " + dir
              + ". Configure release-orchestrator.linux-manual.template-dir com a pasta do instalador.");
        }
        if (!(acesso instanceof LocalHostFsClient localClient)) {
          return Resultado.falha("Cópia automática do instalador só no lab local (host localhost). "
              + "No host remoto, deixe scripts/ e artifacts/ na pasta e rode de novo.");
        }
        localClient.copiarArvore(Path.of(properties.templateDir()), Path.of(dir));
      }
      LinuxManualEnvFactory.PacoteConfig pacote = envFactory.gerar(contexto.instalacao());
      acesso.enviar(host, dir + "/.env", pacote.envFile().getBytes(StandardCharsets.UTF_8));
      acesso.enviar(host, dir + "/templates/context-oracle.xml.template",
          LinuxManualEnvFactory.contextXmlTemplate(TipoBanco.ORACLE).getBytes(StandardCharsets.UTF_8));
      acesso.enviar(host, dir + "/templates/context-sqlserver.xml.template",
          LinuxManualEnvFactory.contextXmlTemplate(TipoBanco.SQLSERVER).getBytes(StandardCharsets.UTF_8));
      boolean skipStart = skipStart(contexto.instalacao().getConfiguracao() == null
          ? null
          : contexto.instalacao().getConfiguracao().getParametros());
      String qdir = JschHostSshClient.shellQuote(dir);
      if (!criar) {
        acesso.exec(host,
            "bash -lc " + JschHostSshClient.shellQuote("cd " + qdir + " && ./scripts/stop.sh || true"),
            Duration.ofSeconds(60));
      }
      String install = "cd " + qdir
          + " && chmod +x scripts/*.sh 2>/dev/null || true"
          + " && ./scripts/install.sh"
          + (skipStart ? "" : " --start");
      HostSshClient.Resultado exec = acesso.exec(host,
          "bash -lc " + JschHostSshClient.shellQuote(install),
          TIMEOUT_INSTALL);
      if (!exec.ok()) {
        return Resultado.falha(resumir("scripts/install.sh falhou (exit " + exec.codigo() + ")", exec));
      }
      String acao = criar ? "instalou" : "atualizou";
      String extra = skipStart ? ", sem start (SKIP_START)" : " e subiu o ambiente";
      return Resultado.ok("REAL: " + acao + " o cliente Linux em " + dir
          + " (" + pacote.vendor() + ") via install.sh" + extra + ".");
    } catch (RuntimeException ex) {
      return Resultado.falha(ex.getMessage());
    }
  }

  private Resultado cicloVida(Contexto contexto, Host host, String dir, HostSshClient acesso) {
    boolean iniciar = contexto.operacao() == OperacaoDeploy.INICIAR;
    String script = iniciar ? "start.sh" : "stop.sh";
    if (!acesso.existe(host, dir + "/scripts/" + script)) {
      return Resultado.falha("Não achei scripts/" + script + " em " + dir
          + ". Crie a instalação no host antes de iniciar/parar.");
    }
    String qdir = JschHostSshClient.shellQuote(dir);
    String cmd = "cd " + qdir
        + " && chmod +x scripts/*.sh 2>/dev/null || true"
        + " && ./scripts/" + script;
    HostSshClient.Resultado exec = acesso.exec(host,
        "bash -lc " + JschHostSshClient.shellQuote(cmd),
        iniciar ? TIMEOUT_START : TIMEOUT_STOP);
    if (!exec.ok()) {
      return Resultado.falha(resumir("scripts/" + script + " falhou (exit " + exec.codigo() + ")", exec));
    }
    String acao = iniciar ? "iniciou" : "parou";
    return Resultado.ok("REAL: " + acao + " o ambiente Linux em " + dir + " via scripts/" + script + ".");
  }

  HostSshClient escolher(Host host, boolean local) {
    if (local && properties.localFsHabilitado()) {
      return localFs;
    }
    return ssh;
  }

  static boolean isLocal(Host host) {
    String h = host.getHostname() == null ? "" : host.getHostname().trim().toLowerCase(Locale.ROOT);
    return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1");
  }

  static boolean skipStart(String parametros) {
    if (parametros == null || parametros.isBlank()) {
      return false;
    }
    String raw = parametros.toUpperCase(Locale.ROOT);
    return raw.contains("SKIP_START=TRUE") || raw.contains("\"SKIP_START\": \"TRUE\"")
        || raw.contains("\"SKIP_START\":\"TRUE\"");
  }

  private static String resumir(String prefixo, HostSshClient.Resultado exec) {
    String err = exec.stderr() == null ? "" : exec.stderr().strip();
    String out = exec.stdout() == null ? "" : exec.stdout().strip();
    String tail = err.isBlank() ? out : err;
    if (tail.length() > 400) {
      tail = tail.substring(tail.length() - 400);
    }
    return prefixo + (tail.isBlank() ? "" : ": " + tail);
  }

  private static String primeiroNaoVazio(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a.trim();
    }
    if (b != null && !b.isBlank()) {
      return b.trim();
    }
    return null;
  }
}
