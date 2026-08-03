package com.nexus.portal.ai.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.portal.ai.dto.response.AiStatusResponse;
import com.nexus.portal.ai.service.AiStatusService;
import com.nexus.portal.shared.security.Permissoes;

@RestController
@RequestMapping("/api/v1/ai")
public class AiStatusController {

  private final AiStatusService aiStatusService;

  public AiStatusController(AiStatusService aiStatusService) {
    this.aiStatusService = aiStatusService;
  }

  @GetMapping("/status")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public AiStatusResponse status() {
    return aiStatusService.status();
  }
}
