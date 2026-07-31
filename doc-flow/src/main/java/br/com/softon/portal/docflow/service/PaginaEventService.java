package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.entity.Pagina;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PaginaEventService {

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

  public void publicar(Pagina pagina, String acao, String usuario) {
    Map<String, Object> evento = new HashMap<>();
    evento.put("id", pagina.getId());
    evento.put("titulo", pagina.getTitulo());
    evento.put("status", pagina.getStatus().name());
    evento.put("acao", acao);
    if (usuario != null && !usuario.isBlank()) {
      evento.put("usuario", usuario);
    }
    assinantes.forEach(emitter -> enviar(emitter, evento));
  }

  private void enviar(SseEmitter emitter, Map<String, Object> evento) {
    try {
      emitter.send(SseEmitter.event().name("pagina").data(evento));
    } catch (IOException | IllegalStateException ex) {
      assinantes.remove(emitter);
      emitter.complete();
    }
  }
}
