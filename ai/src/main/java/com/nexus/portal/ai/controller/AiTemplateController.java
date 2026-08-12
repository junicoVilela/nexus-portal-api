package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.dto.response.AiTemplateRecomendacaoResponse;
import com.nexus.portal.ai.service.AiTemplateRecomendacaoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/templates")
public class AiTemplateController {

  private final AiTemplateRecomendacaoService recomendacaoService;

  public AiTemplateController(AiTemplateRecomendacaoService recomendacaoService) {
    this.recomendacaoService = recomendacaoService;
  }

  @PostMapping("/recomendacao")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public AiTemplateRecomendacaoResponse recomendar(
      @Valid @RequestBody AiTemplateRecomendacaoRequest request) {
    return recomendacaoService.recomendar(request);
  }
}
