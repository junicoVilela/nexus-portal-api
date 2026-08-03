package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.SalvarClienteProdutoModuloRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ClienteProdutoModuloResponse;
import com.nexus.portal.releaseorchestrator.service.ClienteProdutoModuloService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorClienteProdutoModuloController")
@RequestMapping("/api/v1/release-orchestrator/clientes/{clienteId}/produtos/{clienteProdutoId}/modulos")
public class ClienteProdutoModuloController {

  private final ClienteProdutoModuloService service;

  @PreAuthorize(Permissoes.CLIENTE_RO_LER)
  @GetMapping
  public List<ClienteProdutoModuloResponse> listar(@PathVariable UUID clienteId,
      @PathVariable UUID clienteProdutoId) {
    return service.listar(clienteId, clienteProdutoId).stream()
        .map(ClienteProdutoModuloResponse::from).toList();
  }

  @PutMapping("/{moduloProdutoId}")
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public ClienteProdutoModuloResponse salvar(@PathVariable UUID clienteId,
      @PathVariable UUID clienteProdutoId,
      @PathVariable UUID moduloProdutoId,
      @Valid @RequestBody SalvarClienteProdutoModuloRequest request) {
    return ClienteProdutoModuloResponse.from(
        service.salvar(clienteId, clienteProdutoId, moduloProdutoId, request));
  }

  @DeleteMapping("/{moduloProdutoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public void remover(@PathVariable UUID clienteId, @PathVariable UUID clienteProdutoId,
      @PathVariable UUID moduloProdutoId) {
    service.remover(clienteId, clienteProdutoId, moduloProdutoId);
  }
}
