package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.response.EntregaResponse;
import com.nexus.portal.releaseorchestrator.service.GeracaoEntregaService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorGeracaoEntregaController")
@RequestMapping("/api/v1/release-orchestrator/entregas/{entregaId}/geracao")
@Transactional
public class GeracaoEntregaController {

  private final GeracaoEntregaService service;

  /**
   * Dispara a geração assíncrona. Retorna a entrega imediatamente já em
   * EM_GERACAO; cliente deve fazer polling em GET /entregas/{id} para ver
   * a transição para CONCLUIDA ou FALHA.
   */
  @PostMapping("/iniciar")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public EntregaResponse iniciar(@PathVariable UUID entregaId) {
    return EntregaResponse.from(service.iniciar(entregaId));
  }
}
