package com.nexus.portal.releaseorchestrator.integration.docker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cliente HTTP da Docker Engine API (sem SDK). */
public interface DockerEngineClient {

  void ping(String baseUrl);

  void pull(String baseUrl, String imagem, String tag);

  void removerSeExistir(String baseUrl, String nome);

  String criarContainer(String baseUrl, String nome, String imagemRef, Map<String, Object> hostConfig,
      Map<String, Object> exposedPorts, Map<String, String> labels);

  void iniciar(String baseUrl, String nomeOuId);

  void parar(String baseUrl, String nomeOuId);

  static Map<String, Object> portBindings(List<Integer> portas) {
    Map<String, Object> bindings = new LinkedHashMap<>();
    if (portas == null) {
      return bindings;
    }
    for (Integer porta : portas) {
      if (porta == null || porta < 1) {
        continue;
      }
      String key = porta + "/tcp";
      bindings.put(key, List.of(Map.of("HostPort", String.valueOf(porta))));
    }
    return bindings;
  }

  static Map<String, Object> exposedPorts(List<Integer> portas) {
    Map<String, Object> exposed = new LinkedHashMap<>();
    if (portas == null) {
      return exposed;
    }
    for (Integer porta : portas) {
      if (porta == null || porta < 1) {
        continue;
      }
      exposed.put(porta + "/tcp", Map.of());
    }
    return exposed;
  }
}
