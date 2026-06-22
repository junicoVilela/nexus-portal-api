package br.com.softon.portal.releaseorchestrator.integration.publish;

import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import br.com.softon.portal.releaseorchestrator.service.EncryptionService;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publica o pacote em servidor SFTP via JSch (F3 fase 2).
 *
 * <p>Política:
 * <ul>
 *   <li>Autenticação por senha (chave pública = futuro).</li>
 *   <li>{@code strictHostCheck=true} ativa verificação de fingerprint via
 *       arquivo {@code ~/.ssh/known_hosts} do usuário rodando o portal.
 *       Quando desligado, qualquer host é aceito (útil em pré-produção).</li>
 *   <li>Cria diretórios recursivamente em {@code caminhoBase}.</li>
 *   <li>Timeouts: 30 s de conexão, 30 s de operação.</li>
 *   <li>Sobrescreve arquivo de mesmo nome.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SftpPublishStrategy implements PublishStrategy {

  private static final int TIMEOUT_MS = 30_000;
  private static final int DEFAULT_PORT = 22;

  private final EncryptionService encryptionService;

  @Override
  public TipoDestinoEntrega tipo() {
    return TipoDestinoEntrega.SFTP;
  }

  @Override
  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    validar(config);
    Session session = null;
    ChannelSftp sftp = null;
    try {
      session = abrirSessao(config);
      sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect(TIMEOUT_MS);
      navegarECriarPasta(sftp, config.getCaminhoBase());
      String nomeRemoto = pacote.getFileName().toString();
      try (InputStream in = Files.newInputStream(pacote)) {
        sftp.put(in, nomeRemoto, ChannelSftp.OVERWRITE);
      }
      long tamanho = Files.size(pacote);
      String destino = "sftp://" + config.getHost() + ":" + portaOuDefault(config)
          + config.getCaminhoBase() + "/" + nomeRemoto;
      log.info("Pacote publicado em {} ({} bytes)", destino, tamanho);
      return new PublishResult(destino, tamanho, OffsetDateTime.now());
    } catch (JSchException | SftpException | IOException e) {
      throw new PublishException("Falha na transferência SFTP: " + e.getMessage(), e);
    } finally {
      fecharSilencioso(sftp, session);
    }
  }

  @Override
  public void testarConexao(ConfigEntrega config) {
    validar(config);
    Session session = null;
    ChannelSftp sftp = null;
    try {
      session = abrirSessao(config);
      sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect(TIMEOUT_MS);
      sftp.ls("."); // sanidade
      log.info("Teste SFTP OK em {}:{}", config.getHost(), portaOuDefault(config));
    } catch (JSchException | SftpException e) {
      throw new PublishException("Falha na conexão SFTP: " + e.getMessage(), e);
    } finally {
      fecharSilencioso(sftp, session);
    }
  }

  private Session abrirSessao(ConfigEntrega config) throws JSchException {
    String senha = encryptionService.decifrar(config.getSenhaCifrada());
    JSch jsch = new JSch();
    Session session = jsch.getSession(
        config.getUsuario(), config.getHost(), portaOuDefault(config));
    session.setPassword(senha);
    Properties props = new Properties();
    boolean strict = config.getStrictHostCheck() == null || config.getStrictHostCheck();
    props.put("StrictHostKeyChecking", strict ? "yes" : "no");
    session.setConfig(props);
    session.setTimeout(TIMEOUT_MS);
    session.connect(TIMEOUT_MS);
    return session;
  }

  /** Cria diretórios recursivamente e termina em cd para o destino final. */
  private void navegarECriarPasta(ChannelSftp sftp, String caminho) throws SftpException {
    if (caminho == null || caminho.isBlank() || caminho.equals("/")) return;
    boolean absoluto = caminho.startsWith("/");
    String[] partes = caminho.split("/");
    StringBuilder acumulado = new StringBuilder(absoluto ? "/" : "");
    boolean primeiro = true;
    for (String parte : partes) {
      if (parte.isEmpty()) continue;
      if (!primeiro || !absoluto) acumulado.append('/');
      acumulado.append(parte);
      primeiro = false;
      try {
        sftp.stat(acumulado.toString());
      } catch (SftpException stat) {
        sftp.mkdir(acumulado.toString());
      }
    }
    sftp.cd(acumulado.toString());
  }

  private void fecharSilencioso(ChannelSftp sftp, Session session) {
    try {
      if (sftp != null && sftp.isConnected()) sftp.disconnect();
    } catch (Exception e) {
      log.debug("Falha ao fechar canal SFTP: {}", e.getMessage());
    }
    try {
      if (session != null && session.isConnected()) session.disconnect();
    } catch (Exception e) {
      log.debug("Falha ao fechar sessão SSH: {}", e.getMessage());
    }
  }

  private int portaOuDefault(ConfigEntrega config) {
    return config.getPorta() != null ? config.getPorta() : DEFAULT_PORT;
  }

  private void validar(ConfigEntrega c) {
    if (c.getHost() == null || c.getHost().isBlank()) {
      throw new PublishException("Host obrigatório para SFTP.");
    }
    if (c.getUsuario() == null || c.getUsuario().isBlank()) {
      throw new PublishException("Usuário obrigatório para SFTP.");
    }
    if (!c.temSenhaConfigurada()) {
      throw new PublishException("Senha obrigatória para SFTP.");
    }
    if (c.getCaminhoBase() == null || c.getCaminhoBase().isBlank()) {
      throw new PublishException("Caminho base (pasta remota) obrigatório para SFTP.");
    }
  }
}
