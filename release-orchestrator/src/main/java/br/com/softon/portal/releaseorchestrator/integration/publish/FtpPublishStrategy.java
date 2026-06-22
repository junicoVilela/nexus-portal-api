package br.com.softon.portal.releaseorchestrator.integration.publish;

import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Stub do publish via FTP. F3 fase 1 entrega só cadastro + UI; a
 * implementação real (via commons-net) virá em commit subsequente.
 *
 * <p>Validação inline de campos obrigatórios para sinalizar ao operador
 * exatamente o que falta antes de ativar o destino FTP.
 */
@Component
public class FtpPublishStrategy implements PublishStrategy {

  @Override
  public TipoDestinoEntrega tipo() {
    return TipoDestinoEntrega.FTP;
  }

  @Override
  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    validarConfig(config);
    throw new PublishException(
        "Publish via FTP ainda não implementado (F3 fase 2). "
            + "Configure tipoDestino=PASTA até a implementação real entrar no ar.");
  }

  @Override
  public void testarConexao(ConfigEntrega config) {
    validarConfig(config);
    throw new PublishException(
        "Teste de conexão FTP ainda não implementado (F3 fase 2). "
            + "Os campos cadastrados foram validados — host, porta, usuário e senha estão OK.");
  }

  private void validarConfig(ConfigEntrega c) {
    if (c.getHost() == null || c.getHost().isBlank()) {
      throw new PublishException("Host obrigatório para FTP.");
    }
    if (c.getPorta() != null && (c.getPorta() < 1 || c.getPorta() > 65535)) {
      throw new PublishException("Porta inválida: " + c.getPorta());
    }
    if (c.getUsuario() == null || c.getUsuario().isBlank()) {
      throw new PublishException("Usuário obrigatório para FTP.");
    }
    if (!c.temSenhaConfigurada()) {
      throw new PublishException("Senha obrigatória para FTP.");
    }
  }
}
