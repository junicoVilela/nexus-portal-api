package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Publicacao;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PublicacaoEventService {

  private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;
  private final CopyOnWriteArrayList<SseEmitter> assinantes = new CopyOnWriteArrayList<>();

  public SseEmitter inscrever() {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
    assinantes.add(emitter);
    emitter.onCompletion(() -> assinantes.remove(emitter));
    emitter.onTimeout(() -> assinantes.remove(emitter));
    emitter.onError(error -> assinantes.remove(emitter));
    try {
      emitter.send(SseEmitter.event().name("conectado").data(Map.of("status", "OK")));
    } catch (IOException ex) {
      assinantes.remove(emitter);
      emitter.completeWithError(ex);
    }
    return emitter;
  }

  public void publicar(Publicacao publicacao) {
    Map<String, Object> evento = Map.of(
        "id", publicacao.getId(),
        "clienteId", publicacao.getCliente().getId(),
        "status", publicacao.getStatus().name(),
        "versao", publicacao.getVersao());
    assinantes.forEach(emitter -> enviar(emitter, evento));
  }

  private void enviar(SseEmitter emitter, Map<String, Object> evento) {
    try {
      emitter.send(SseEmitter.event().name("publicacao").data(evento));
    } catch (IOException | IllegalStateException ex) {
      assinantes.remove(emitter);
      emitter.complete();
    }
  }
}
