package br.com.softon.portal.releaseorchestrator.integration.publish;

import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import java.nio.file.Path;

/**
 * Estratégia de publicação do pacote gerado no destino do cliente
 * (F3.* — distribuição remota).
 *
 * <p>Implementações:
 * <ul>
 *   <li>{@link LocalFolderPublishStrategy} — copia para pasta local (MVP, S1–S11).</li>
 *   <li>{@link FtpPublishStrategy} — upload via FTP (stub).</li>
 *   <li>{@link SftpPublishStrategy} — upload via SFTP (stub).</li>
 * </ul>
 *
 * <p>O dispatcher {@link PublishService} escolhe a estratégia certa pelo
 * {@link ConfigEntrega#getTipoDestino()}.
 */
public interface PublishStrategy {

  TipoDestinoEntrega tipo();

  /**
   * Publica o pacote ZIP em {@code pacote} para o destino configurado em
   * {@code config}. Bloqueante. Caller é responsável por rodar em thread
   * separada se necessário.
   *
   * @return resultado com path/URL final no destino + tamanho transferido.
   * @throws PublishException quando falha (caller decide se marca entrega
   *         como FALHA ou agenda retry).
   */
  PublishResult publicar(ConfigEntrega config, Path pacote);

  /**
   * Testa a conexão com o destino remoto sem enviar dados.
   *
   * @throws PublishException quando o teste falha — mensagem amigável.
   */
  void testarConexao(ConfigEntrega config);
}
