package com.nexus.portal.releaseorchestrator.integration.publish;

import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Dispatcher de {@link PublishStrategy} pelo
 * {@link ConfigEntrega#getTipoDestino()}. Coleciona todas as estratégias
 * registradas como bean Spring e expõe métodos thin para o caller —
 * {@code GeracaoEntregaService} no fluxo automático, {@code ConfigEntregaService}
 * no teste de conexão.
 */
@Service
public class PublishService {

  private final Map<TipoDestinoEntrega, PublishStrategy> estrategias;

  public PublishService(List<PublishStrategy> beans) {
    this.estrategias = new EnumMap<>(TipoDestinoEntrega.class);
    for (PublishStrategy s : beans) {
      estrategias.put(s.tipo(), s);
    }
  }

  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    return estrategiaPara(config).publicar(config, pacote);
  }

  public void testarConexao(ConfigEntrega config) {
    estrategiaPara(config).testarConexao(config);
  }

  private PublishStrategy estrategiaPara(ConfigEntrega config) {
    PublishStrategy s = estrategias.get(config.getTipoDestino());
    if (s == null) {
      throw new PublishException(
          "Tipo de destino " + config.getTipoDestino() + " sem estratégia registrada.");
    }
    return s;
  }
}
