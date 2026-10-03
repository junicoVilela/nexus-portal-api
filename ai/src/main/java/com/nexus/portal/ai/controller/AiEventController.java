package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.service.AiEventService;
import com.nexus.portal.shared.security.Permissoes;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai")
public class AiEventController {

  private final AiEventService aiEventService;

  public AiEventController(AiEventService aiEventService) {
    this.aiEventService = aiEventService;
  }

  @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize(Permissoes.PAGINA_LER)
  public SseEmitter eventos(Principal principal) {
    return aiEventService.inscrever(principal.getName());
  }
}
