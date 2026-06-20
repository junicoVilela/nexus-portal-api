package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AtualizarEntregaRascunhoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.CriarEntregaRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.EntregaResponse;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import br.com.softon.portal.releaseorchestrator.service.EntregaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorEntregaController")
@RequestMapping("/api/v1/release-orchestrator/entregas")
@PreAuthorize(SecurityRoles.WRITE)
public class EntregaController {

  private final EntregaService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public PageResponse<EntregaResponse> listar(
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(required = false) UUID produtoId,
      @RequestParam(required = false) StatusEntrega status,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("createdAt", "dataConclusao", "status"),
        Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        service.listar(clienteId, produtoId, status,
            PageableUtils.of(page, size, sortOrder)),
        EntregaResponse::from);
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public EntregaResponse buscar(@PathVariable UUID id) {
    return EntregaResponse.from(service.buscar(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EntregaResponse criar(@Valid @RequestBody CriarEntregaRequest request) {
    return EntregaResponse.from(service.criar(request));
  }

  @PutMapping("/{id}/rascunho")
  public EntregaResponse atualizarRascunho(@PathVariable UUID id,
      @Valid @RequestBody AtualizarEntregaRascunhoRequest request) {
    return EntregaResponse.from(service.atualizarRascunho(id, request));
  }

  @PostMapping("/{id}/cancelar")
  public EntregaResponse cancelar(@PathVariable UUID id) {
    return EntregaResponse.from(service.cancelar(id));
  }
}
