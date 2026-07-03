package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.response.EntregaResponse;
import br.com.softon.portal.releaseorchestrator.service.GeracaoEntregaService;
import br.com.softon.portal.shared.security.Permissoes;
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
