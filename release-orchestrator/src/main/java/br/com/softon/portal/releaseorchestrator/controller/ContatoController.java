package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.ContatoRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ContatoResponse;
import br.com.softon.portal.releaseorchestrator.service.ContatoService;
import br.com.softon.portal.shared.security.SecurityRoles;
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
@RestController("orchestratorContatoController")
@RequestMapping("/api/v1/release-orchestrator/clientes/{clienteId}/contatos")
@PreAuthorize(SecurityRoles.WRITE)
public class ContatoController {

  private final ContatoService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public List<ContatoResponse> listar(@PathVariable UUID clienteId) {
    return service.listar(clienteId).stream().map(ContatoResponse::from).toList();
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public ContatoResponse buscar(@PathVariable UUID clienteId, @PathVariable UUID id) {
    return ContatoResponse.from(service.buscar(clienteId, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ContatoResponse criar(@PathVariable UUID clienteId,
      @Valid @RequestBody ContatoRequest request) {
    return ContatoResponse.from(service.criar(clienteId, request));
  }

  @PutMapping("/{id}")
  public ContatoResponse atualizar(@PathVariable UUID clienteId, @PathVariable UUID id,
      @Valid @RequestBody ContatoRequest request) {
    return ContatoResponse.from(service.atualizar(clienteId, id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable UUID clienteId, @PathVariable UUID id) {
    service.excluir(clienteId, id);
  }
}
