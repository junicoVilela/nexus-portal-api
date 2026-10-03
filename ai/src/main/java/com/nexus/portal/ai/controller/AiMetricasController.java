package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.response.AiMetricasResponse;
import com.nexus.portal.ai.service.AiMetricasService;
import com.nexus.portal.shared.security.Permissoes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/metricas")
public class AiMetricasController {

  private final AiMetricasService metricasService;

  public AiMetricasController(AiMetricasService metricasService) {
    this.metricasService = metricasService;
  }

  /** Painel de qualidade da IA (observabilidade, perfil administrativo). */
  @GetMapping
  @PreAuthorize(Permissoes.AUDITORIA_VISUALIZAR)
  public AiMetricasResponse metricas(@RequestParam(defaultValue = "30") int dias) {
    return metricasService.calcular(dias);
  }
}
