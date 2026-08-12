package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiJob;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiEventService {

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

  public void publicarJob(AiJob job) {
    Map<String, Object> evento = new LinkedHashMap<>();
    evento.put("jobId", job.getId());
    evento.put("sessaoId", job.getSessao().getId());
    evento.put("status", job.getStatus().name());
    evento.put("etapa", job.getEtapa().name());
    evento.put("progresso", job.getProgresso());
    evento.put("tentativa", job.getTentativa());
    if (job.getDiagnosticoId() != null) {
      evento.put("diagnosticoId", job.getDiagnosticoId());
    }
    assinantes.forEach(emitter -> enviar(emitter, evento));
  }

  private void enviar(SseEmitter emitter, Map<String, Object> evento) {
    try {
      emitter.send(SseEmitter.event().name("ai-job").data(evento));
    } catch (IOException | IllegalStateException ex) {
      assinantes.remove(emitter);
      emitter.complete();
    }
  }
}
