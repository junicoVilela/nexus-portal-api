package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.CalcularDeltaRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.DeltaResumoResponse;
import br.com.softon.portal.releaseorchestrator.dto.response.EntregaModuloArtefatoResponse;
import br.com.softon.portal.releaseorchestrator.service.DeltaEntregaService;
import br.com.softon.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorDeltaEntregaController")
@RequestMapping("/api/v1/release-orchestrator/entregas/{entregaId}/delta")
public class DeltaEntregaController {

  private final DeltaEntregaService service;

  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping
  public List<EntregaModuloArtefatoResponse> listar(@PathVariable UUID entregaId) {
    return service.listar(entregaId).stream()
        .map(EntregaModuloArtefatoResponse::from).toList();
  }

  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping("/resumo")
  public DeltaResumoResponse resumo(@PathVariable UUID entregaId) {
    return service.resumo(entregaId);
  }

  @PostMapping("/calcular")
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public DeltaResumoResponse calcular(@PathVariable UUID entregaId,
      @Valid @RequestBody(required = false) CalcularDeltaRequest request) {
    return service.calcular(entregaId, request);
  }
}
