package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarClienteProdutoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ContratarProdutoRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ClienteProdutoResponse;
import com.nexus.portal.releaseorchestrator.service.ClienteProdutoService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorClienteProdutoController")
@RequestMapping("/api/v1/release-orchestrator/clientes/{clienteId}/produtos")
public class ClienteProdutoController {

  private final ClienteProdutoService service;

  @PreAuthorize(Permissoes.CLIENTE_RO_LER)
  @GetMapping
  public List<ClienteProdutoResponse> listar(@PathVariable UUID clienteId) {
    return service.listar(clienteId).stream().map(ClienteProdutoResponse::from).toList();
  }

  @PreAuthorize(Permissoes.CLIENTE_RO_LER)
  @GetMapping("/{id}")
  public ClienteProdutoResponse buscar(@PathVariable UUID clienteId, @PathVariable UUID id) {
    return ClienteProdutoResponse.from(service.buscar(clienteId, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public ClienteProdutoResponse contratar(@PathVariable UUID clienteId,
      @Valid @RequestBody ContratarProdutoRequest request) {
    return ClienteProdutoResponse.from(service.contratar(clienteId, request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public ClienteProdutoResponse atualizar(@PathVariable UUID clienteId, @PathVariable UUID id,
      @Valid @RequestBody AtualizarClienteProdutoRequest request) {
    return ClienteProdutoResponse.from(service.atualizar(clienteId, id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public void rescindir(@PathVariable UUID clienteId, @PathVariable UUID id) {
    service.rescindir(clienteId, id);
  }
}
