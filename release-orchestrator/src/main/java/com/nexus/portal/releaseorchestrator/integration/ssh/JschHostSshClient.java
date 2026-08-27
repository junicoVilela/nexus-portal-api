package com.nexus.portal.releaseorchestrator.integration.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.service.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class JschHostSshClient implements HostSshClient {

  private static final int DEFAULT_PORT = 22;
  private static final int CONNECT_MS = 20_000;

  private final EncryptionService encryptionService;

  @Override
  public boolean existe(Host host, String caminhoRemoto) {
    Resultado r = exec(host, "test -e " + shellQuote(caminhoRemoto), Duration.ofSeconds(20));
    return r.ok();
  }

  @Override
  public void enviar(Host host, String caminhoRemoto, byte[] conteudo) {
    Session session = null;
    ChannelSftp sftp = null;
    try {
      session = abrir(host);
      sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect(CONNECT_MS);
      String dir = caminhoRemoto.contains("/")
          ? caminhoRemoto.substring(0, caminhoRemoto.lastIndexOf('/'))
          : ".";
      mkdirP(sftp, dir);
      try (InputStream in = new ByteArrayInputStream(conteudo)) {
        sftp.put(in, caminhoRemoto, ChannelSftp.OVERWRITE);
      }
    } catch (JSchException | SftpException | IOException e) {
      throw new SshHostException("Não enviou " + caminhoRemoto + " via SSH: " + e.getMessage(), e);
    } finally {
      if (sftp != null && sftp.isConnected()) {
        sftp.disconnect();
      }
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
    }
  }

  @Override
  public Resultado exec(Host host, String comando, Duration timeout) {
    Session session = null;
    ChannelExec channel = null;
    try {
      session = abrir(host);
      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(comando);
      channel.setInputStream(null);
      ByteArrayOutputStream err = new ByteArrayOutputStream();
      channel.setErrStream(err);
      InputStream in = channel.getInputStream();
      channel.connect(CONNECT_MS);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[2048];
      long deadline = System.nanoTime() + timeout.toNanos();
      while (true) {
        while (in.available() > 0) {
          int n = in.read(buf);
          if (n < 0) {
            break;
          }
          out.write(buf, 0, n);
        }
        if (channel.isClosed()) {
          while (in.available() > 0) {
            int n = in.read(buf);
            if (n < 0) {
              break;
            }
            out.write(buf, 0, n);
          }
          break;
        }
        if (System.nanoTime() > deadline) {
          throw new SshHostException("Timeout SSH (" + timeout.toSeconds() + "s) em " + host.getHostname() + ".");
        }
        Thread.sleep(80);
      }
      int code = channel.getExitStatus();
      return new Resultado(
          code,
          out.toString(StandardCharsets.UTF_8),
          err.toString(StandardCharsets.UTF_8));
    } catch (SshHostException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SshHostException("SSH interrompido em " + host.getHostname() + ".", e);
    } catch (JSchException | IOException e) {
      throw new SshHostException("Falha SSH em " + host.getHostname() + ": " + e.getMessage(), e);
    } finally {
      if (channel != null && channel.isConnected()) {
        channel.disconnect();
      }
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
    }
  }

  private Session abrir(Host host) throws JSchException {
    if (host.getUsuarioConexao() == null || host.getUsuarioConexao().isBlank()) {
      throw new SshHostException("Host sem usuário SSH (usuarioConexao).");
    }
    int porta = host.getPortaConexao() != null ? host.getPortaConexao() : DEFAULT_PORT;
    JSch jsch = new JSch();
    String cred = host.getCredencialRef();
    String senha = resolverSenha(cred);
    Path chave = resolverChave(cred, senha);
    if (chave != null) {
      jsch.addIdentity(chave.toString());
    }
    Session session = jsch.getSession(host.getUsuarioConexao().trim(), host.getHostname(), porta);
    if (senha != null && chave == null) {
      session.setPassword(senha);
    }
    Properties props = new Properties();
    props.put("StrictHostKeyChecking", "no");
    session.setConfig(props);
    session.setTimeout(CONNECT_MS);
    session.connect(CONNECT_MS);
    return session;
  }

  private String resolverSenha(String credencialRef) {
    if (credencialRef == null || credencialRef.isBlank()) {
      return null;
    }
    String raw = credencialRef.trim();
    if (pareceCaminho(raw)) {
      return null;
    }
    try {
      String dec = encryptionService.decifrar(raw);
      if (dec != null && !dec.isBlank()) {
        return dec;
      }
    } catch (RuntimeException ignored) {
      return raw;
    }
    return raw;
  }

  private static Path resolverChave(String credencialRef, String senhaJaResolvida) {
    if (credencialRef != null && pareceCaminho(credencialRef.trim())) {
      Path p = Path.of(expandHome(credencialRef.trim()));
      if (Files.isRegularFile(p)) {
        return p;
      }
      throw new SshHostException("Chave SSH não encontrada: " + credencialRef);
    }
    if (senhaJaResolvida != null) {
      return null;
    }
    Path idRsa = Path.of(System.getProperty("user.home"), ".ssh", "id_rsa");
    Path idEd = Path.of(System.getProperty("user.home"), ".ssh", "id_ed25519");
    if (Files.isRegularFile(idEd)) {
      return idEd;
    }
    if (Files.isRegularFile(idRsa)) {
      return idRsa;
    }
    throw new SshHostException(
        "Sem credencial SSH: informe credencialRef (senha cifrada ou caminho da chave) "
            + "ou coloque uma chave em ~/.ssh.");
  }

  private static boolean pareceCaminho(String value) {
    return value.startsWith("/") || value.startsWith("~") || value.contains("id_rsa")
        || value.contains("id_ed25519");
  }

  private static String expandHome(String path) {
    if (path.startsWith("~/")) {
      return System.getProperty("user.home") + path.substring(1);
    }
    return path;
  }

  private static void mkdirP(ChannelSftp sftp, String caminho) throws SftpException {
    if (caminho == null || caminho.isBlank() || caminho.equals("/")) {
      return;
    }
    boolean absoluto = caminho.startsWith("/");
    String[] partes = caminho.split("/");
    StringBuilder acumulado = new StringBuilder(absoluto ? "/" : "");
    boolean primeiro = true;
    for (String parte : partes) {
      if (parte.isEmpty()) {
        continue;
      }
      if (!primeiro || !absoluto) {
        acumulado.append('/');
      }
      acumulado.append(parte);
      primeiro = false;
      try {
        sftp.stat(acumulado.toString());
      } catch (SftpException e) {
        sftp.mkdir(acumulado.toString());
      }
    }
  }

  public static String shellQuote(String value) {
    return "'" + value.replace("'", "'\\''") + "'";
  }
}
