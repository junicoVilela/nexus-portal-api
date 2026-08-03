package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.SalvarClienteFuncionalidadeRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ClienteFuncionalidadeResponse;
import com.nexus.portal.releaseorchestrator.service.ClienteFuncionalidadeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorClienteFuncionalidadeController")
@RequestMapping("/api/v1/release-orchestrator/clientes/{clienteId}/funcionalidades")
public class ClienteFuncionalidadeController {

  private final ClienteFuncionalidadeService service;

  /**
   * Lista os vínculos do cliente. Se {@code produtoId} for informado, restringe
   * ao produto. Funcionalidades não presentes na matriz não aparecem aqui;
   * a UI deve renderizá-las como desabilitadas.
   */
  @PreAuthorize(Permissoes.CLIENTE_RO_LER)
  @GetMapping
  public List<ClienteFuncionalidadeResponse> listar(@PathVariable UUID clienteId,
      @RequestParam(required = false) UUID produtoId) {
    var lista = produtoId != null
        ? service.listarPorClienteEProduto(clienteId, produtoId)
        : service.listarPorCliente(clienteId);
    return lista.stream().map(ClienteFuncionalidadeResponse::from).toList();
  }

  @PutMapping("/{funcionalidadeId}")
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public ClienteFuncionalidadeResponse salvar(@PathVariable UUID clienteId,
      @PathVariable UUID funcionalidadeId,
      @Valid @RequestBody SalvarClienteFuncionalidadeRequest request) {
    return ClienteFuncionalidadeResponse.from(
        service.salvar(clienteId, funcionalidadeId, request));
  }

  @DeleteMapping("/{funcionalidadeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_RO_EDITAR)
  public void remover(@PathVariable UUID clienteId, @PathVariable UUID funcionalidadeId) {
    service.remover(clienteId, funcionalidadeId);
  }
}
