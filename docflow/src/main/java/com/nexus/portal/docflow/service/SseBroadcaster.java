package com.nexus.portal.docflow.service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Distribui eventos para os assinantes conectados, respeitando o escopo de
 * cliente de cada um. O escopo é fotografado no momento da inscrição: a
 * conexão vive no máximo {@link #TIMEOUT_MILLIS} e uma mudança de escopo passa
 * a valer na reconexão seguinte.
 *
 * <p>A lista vive no processo — com mais de uma instância da API cada uma
 * notifica apenas os seus assinantes.
 */
class SseBroadcaster {

  static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

  private final CopyOnWriteArrayList<Assinante> assinantes = new CopyOnWriteArrayList<>();

  /**
   * @param clientesPermitidos {@code Optional.empty()} = sem restrição de escopo;
   *     caso contrário, o assinante só recebe eventos dos clientes listados.
   */
  SseEmitter inscrever(Optional<Set<UUID>> clientesPermitidos) {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
    Assinante assinante = new Assinante(emitter, clientesPermitidos);
    assinantes.add(assinante);
    emitter.onCompletion(() -> assinantes.remove(assinante));
    emitter.onTimeout(() -> assinantes.remove(assinante));
    emitter.onError(error -> assinantes.remove(assinante));
    try {
      emitter.send(SseEmitter.event().name("conectado").data(Map.of("status", "OK")));
    } catch (IOException ex) {
      assinantes.remove(assinante);
      emitter.completeWithError(ex);
    }
    return emitter;
  }

  /**
   * @param clienteId cliente dono do evento; {@code null} quando o evento não é
   *     de um cliente específico e vale para todos os assinantes.
   */
  void publicar(String nomeEvento, Map<String, Object> dados, UUID clienteId) {
    for (Assinante assinante : assinantes) {
      if (assinante.podeReceber(clienteId)) {
        enviar(assinante, nomeEvento, dados);
      }
    }
  }

  private void enviar(Assinante assinante, String nomeEvento, Map<String, Object> dados) {
    try {
      assinante.emitter().send(SseEmitter.event().name(nomeEvento).data(dados));
    } catch (IOException | IllegalStateException ex) {
      assinantes.remove(assinante);
      assinante.emitter().complete();
    }
  }

  /**
   * Um evento sem cliente vale para todos; com cliente, só chega a quem tem
   * escopo aberto ou o cliente na whitelist.
   */
  static boolean podeReceber(Optional<Set<UUID>> clientesPermitidos, UUID clienteId) {
    return clienteId == null
        || clientesPermitidos.map(ids -> ids.contains(clienteId)).orElse(true);
  }

  private record Assinante(SseEmitter emitter, Optional<Set<UUID>> clientesPermitidos) {

    boolean podeReceber(UUID clienteId) {
      return SseBroadcaster.podeReceber(clientesPermitidos, clienteId);
    }
  }
}
