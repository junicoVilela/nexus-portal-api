package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Pagina;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Stream do fluxo editorial. A página é um objeto interno da redação — não
 * pertence a um cliente —, então o evento vai para todos os assinantes com
 * permissão de leitura de página.
 */
@Service
public class PaginaEventService {

  private final SseBroadcaster broadcaster = new SseBroadcaster();

  public SseEmitter inscrever() {
    return broadcaster.inscrever(Optional.empty());
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
    broadcaster.publicar("pagina", evento, null);
  }
}
