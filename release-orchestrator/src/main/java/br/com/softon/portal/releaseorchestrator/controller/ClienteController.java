package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusClienteRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ClienteRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ClienteResponse;
import br.com.softon.portal.releaseorchestrator.service.ClienteService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import br.com.softon.portal.shared.security.SecurityRoles;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorClienteController")
@RequestMapping("/api/v1/release-orchestrator/clientes")
@PreAuthorize(SecurityRoles.WRITE)
public class ClienteController {

  private final ClienteService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClienteResponse criar(@Valid @RequestBody ClienteRequest request) {
    return ClienteResponse.from(service.criar(request));
  }

  @PutMapping("/{id}")
  public ClienteResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody ClienteRequest request) {
    return ClienteResponse.from(service.atualizar(id, request));
  }

  @PatchMapping("/{id}/status")
  public ClienteResponse alterarStatus(@PathVariable UUID id,
      @Valid @RequestBody AlterarStatusClienteRequest request) {
    return ClienteResponse.from(service.alterarStatus(id, request.ativo()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable UUID id) {
    service.excluir(id);
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public ClienteResponse buscar(@PathVariable UUID id) {
    return ClienteResponse.from(service.buscar(id));
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public PageResponse<ClienteResponse> listar(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean ativo,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("nome", "sigla", "ativo", "createdAt", "updatedAt"),
        Sort.by("nome"));
    return PageResponse.from(
        service.listar(q, ativo, PageableUtils.of(page, size, sortOrder)),
        ClienteResponse::from);
  }
}
