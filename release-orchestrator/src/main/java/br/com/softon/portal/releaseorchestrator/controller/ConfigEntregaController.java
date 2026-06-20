package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.ConfigEntregaRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ConfigEntregaResponse;
import br.com.softon.portal.releaseorchestrator.service.ConfigEntregaService;
import br.com.softon.portal.shared.security.SecurityRoles;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorConfigEntregaController")
@RequestMapping("/api/v1/release-orchestrator/clientes/{clienteId}/config-entrega")
@PreAuthorize(SecurityRoles.WRITE)
public class ConfigEntregaController {

  private final ConfigEntregaService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public ConfigEntregaResponse buscar(@PathVariable UUID clienteId) {
    return ConfigEntregaResponse.from(service.buscar(clienteId));
  }

  @PutMapping
  public ConfigEntregaResponse salvar(@PathVariable UUID clienteId,
      @Valid @RequestBody ConfigEntregaRequest request) {
    return ConfigEntregaResponse.from(service.salvar(clienteId, request));
  }
}
