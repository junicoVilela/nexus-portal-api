package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.ConfigEntregaRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ConfigEntregaResponse;
import com.nexus.portal.releaseorchestrator.service.ConfigEntregaService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorConfigEntregaController")
@RequestMapping("/api/v1/release-orchestrator/clientes/{clienteId}/config-entrega")
public class ConfigEntregaController {

  private final ConfigEntregaService service;

  @PreAuthorize(Permissoes.CLIENTE_RO_LER)
  @GetMapping
  public ConfigEntregaResponse buscar(@PathVariable UUID clienteId) {
    return ConfigEntregaResponse.from(service.buscar(clienteId));
  }

  @PutMapping
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public ConfigEntregaResponse salvar(@PathVariable UUID clienteId,
      @Valid @RequestBody ConfigEntregaRequest request) {
    return ConfigEntregaResponse.from(service.salvar(clienteId, request));
  }

  /**
   * Testa a conexão com o destino salvo. Retorna mensagem amigável em texto.
   * Erro de configuração ou conexão é mapeado para 422 via BusinessException.
   */
  @PostMapping("/testar")
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public String testar(@PathVariable UUID clienteId) {
    return service.testarConexao(clienteId);
  }
}
