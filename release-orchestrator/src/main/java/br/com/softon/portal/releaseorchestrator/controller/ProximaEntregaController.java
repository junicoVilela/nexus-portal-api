package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusProximaEntregaRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ProximaEntregaRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ProximaEntregaResponse;
import br.com.softon.portal.releaseorchestrator.entity.StatusProximaEntrega;
import br.com.softon.portal.releaseorchestrator.service.ProximaEntregaService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import br.com.softon.portal.shared.security.SecurityRoles;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
@RestController("orchestratorProximaEntregaController")
@RequestMapping("/api/v1/release-orchestrator/proximas-entregas")
@PreAuthorize(SecurityRoles.WRITE)
public class ProximaEntregaController {

  private final ProximaEntregaService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public PageResponse<ProximaEntregaResponse> listar(
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(required = false) UUID produtoId,
      @RequestParam(required = false) StatusProximaEntrega status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDe,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAte,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("dataPrevista", "prioridade", "status", "createdAt"),
        Sort.by("dataPrevista"));
    return PageResponse.from(
        service.listar(clienteId, produtoId, status, dataDe, dataAte,
            PageableUtils.of(page, size, sortOrder)),
        ProximaEntregaResponse::from);
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public ProximaEntregaResponse buscar(@PathVariable UUID id) {
    return ProximaEntregaResponse.from(service.buscar(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProximaEntregaResponse criar(@Valid @RequestBody ProximaEntregaRequest request) {
    return ProximaEntregaResponse.from(service.criar(request));
  }

  @PutMapping("/{id}")
  public ProximaEntregaResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody ProximaEntregaRequest request) {
    return ProximaEntregaResponse.from(service.atualizar(id, request));
  }

  @PatchMapping("/{id}/status")
  public ProximaEntregaResponse alterarStatus(@PathVariable UUID id,
      @Valid @RequestBody AlterarStatusProximaEntregaRequest request) {
    return ProximaEntregaResponse.from(service.alterarStatus(id, request.status()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable UUID id) {
    service.excluir(id);
  }
}
