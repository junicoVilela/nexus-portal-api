package com.nexus.portal.releaseorchestrator.integration.publish;

import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import com.nexus.portal.releaseorchestrator.service.EncryptionService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Component;

/**
 * Publica o pacote em servidor FTP via Apache Commons Net (F3 fase 2).
 *
 * <p>Política:
 * <ul>
 *   <li>Modo passivo por default ({@link ConfigEntrega#getModoPassivo()}).</li>
 *   <li>Transferência binária (ZIP).</li>
 *   <li>Cria diretórios recursivamente em {@code caminhoBase}.</li>
 *   <li>Timeout de 30 s para conexão e operações de comando.</li>
 *   <li>Sobrescreve arquivo de mesmo nome (geração é idempotente).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FtpPublishStrategy implements PublishStrategy {

  private static final int TIMEOUT_MS = 30_000;
  private static final int DEFAULT_PORT = 21;

  private final EncryptionService encryptionService;

  @Override
  public TipoDestinoEntrega tipo() {
    return TipoDestinoEntrega.FTP;
  }

  @Override
  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    validar(config);
    FTPClient client = novoClient(config);
    try {
      conectarELogar(client, config);
      navegarECriarPasta(client, config.getCaminhoBase());
      client.setFileType(FTP.BINARY_FILE_TYPE);
      String nomeRemoto = pacote.getFileName().toString();
      try (InputStream in = Files.newInputStream(pacote)) {
        if (!client.storeFile(nomeRemoto, in)) {
          throw new PublishException("FTP storeFile retornou false. Reply: "
              + client.getReplyString());
        }
      }
      long tamanho = Files.size(pacote);
      String destino = "ftp://" + config.getHost() + ":" + portaOuDefault(config)
          + config.getCaminhoBase() + "/" + nomeRemoto;
      log.info("Pacote publicado em {} ({} bytes)", destino, tamanho);
      return new PublishResult(destino, tamanho, OffsetDateTime.now());
    } catch (IOException e) {
      throw new PublishException("Falha na transferência FTP: " + e.getMessage(), e);
    } finally {
      desconectarSilencioso(client);
    }
  }

  @Override
  public void testarConexao(ConfigEntrega config) {
    validar(config);
    FTPClient client = novoClient(config);
    try {
      conectarELogar(client, config);
      // listFiles na raiz para garantir que o login deu canal de dados ok
      client.listNames();
      log.info("Teste FTP OK em {}:{}", config.getHost(), portaOuDefault(config));
    } catch (IOException e) {
      throw new PublishException("Falha na conexão FTP: " + e.getMessage(), e);
    } finally {
      desconectarSilencioso(client);
    }
  }

  private FTPClient novoClient(ConfigEntrega config) {
    FTPClient c = new FTPClient();
    c.setConnectTimeout(TIMEOUT_MS);
    c.setDefaultTimeout(TIMEOUT_MS);
    return c;
  }

  private void conectarELogar(FTPClient client, ConfigEntrega config) throws IOException {
    client.connect(config.getHost(), portaOuDefault(config));
    int reply = client.getReplyCode();
    if (!FTPReply.isPositiveCompletion(reply)) {
      throw new PublishException("FTP rejeitou a conexão. Reply: " + reply);
    }
    String senha = encryptionService.decifrar(config.getSenhaCifrada());
    if (!client.login(config.getUsuario(), senha)) {
      throw new PublishException("FTP login falhou para usuário " + config.getUsuario());
    }
    if (config.getModoPassivo() == null || config.getModoPassivo()) {
      client.enterLocalPassiveMode();
    } else {
      client.enterLocalActiveMode();
    }
    client.setSoTimeout(TIMEOUT_MS);
  }

  /** Cria diretórios recursivamente e muda pra ele. */
  private void navegarECriarPasta(FTPClient client, String caminho) throws IOException {
    if (caminho == null || caminho.isBlank() || caminho.equals("/")) return;
    String[] partes = caminho.split("/");
    for (String parte : partes) {
      if (parte.isEmpty()) continue;
      if (!client.changeWorkingDirectory(parte)) {
        if (!client.makeDirectory(parte)) {
          throw new PublishException("FTP não conseguiu criar/abrir pasta '" + parte
              + "' (path " + caminho + "). Reply: " + client.getReplyString());
        }
        if (!client.changeWorkingDirectory(parte)) {
          throw new PublishException("FTP criou mas não conseguiu entrar em '" + parte + "'.");
        }
      }
    }
  }

  private void desconectarSilencioso(FTPClient client) {
    try {
      if (client.isConnected()) {
        client.logout();
        client.disconnect();
      }
    } catch (IOException e) {
      log.debug("Falha ao desconectar FTP: {}", e.getMessage());
    }
  }

  private int portaOuDefault(ConfigEntrega config) {
    return config.getPorta() != null ? config.getPorta() : DEFAULT_PORT;
  }

  private void validar(ConfigEntrega c) {
    if (c.getHost() == null || c.getHost().isBlank()) {
      throw new PublishException("Host obrigatório para FTP.");
    }
    if (c.getUsuario() == null || c.getUsuario().isBlank()) {
      throw new PublishException("Usuário obrigatório para FTP.");
    }
    if (!c.temSenhaConfigurada()) {
      throw new PublishException("Senha obrigatória para FTP.");
    }
    if (c.getCaminhoBase() == null || c.getCaminhoBase().isBlank()) {
      throw new PublishException("Caminho base (pasta remota) obrigatório para FTP.");
    }
  }
}
