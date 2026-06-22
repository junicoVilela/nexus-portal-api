package br.com.softon.portal.releaseorchestrator.integration.publish;

import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Stub do publish via SFTP. Mesma estratégia do {@link FtpPublishStrategy}
 * para F3 fase 1.
 */
@Component
public class SftpPublishStrategy implements PublishStrategy {

  @Override
  public TipoDestinoEntrega tipo() {
    return TipoDestinoEntrega.SFTP;
  }

  @Override
  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    validarConfig(config);
    throw new PublishException(
        "Publish via SFTP ainda não implementado (F3 fase 2). "
            + "Configure tipoDestino=PASTA até a implementação real entrar no ar.");
  }

  @Override
  public void testarConexao(ConfigEntrega config) {
    validarConfig(config);
    throw new PublishException(
        "Teste de conexão SFTP ainda não implementado (F3 fase 2). "
            + "Os campos cadastrados foram validados — host, porta, usuário e senha estão OK.");
  }

  private void validarConfig(ConfigEntrega c) {
    if (c.getHost() == null || c.getHost().isBlank()) {
      throw new PublishException("Host obrigatório para SFTP.");
    }
    if (c.getPorta() != null && (c.getPorta() < 1 || c.getPorta() > 65535)) {
      throw new PublishException("Porta inválida: " + c.getPorta());
    }
    if (c.getUsuario() == null || c.getUsuario().isBlank()) {
      throw new PublishException("Usuário obrigatório para SFTP.");
    }
    if (!c.temSenhaConfigurada()) {
      throw new PublishException("Senha obrigatória para SFTP.");
    }
  }
}
