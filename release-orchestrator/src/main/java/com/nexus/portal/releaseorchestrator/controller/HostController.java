package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarStatusHostRequest;
import com.nexus.portal.releaseorchestrator.dto.request.HostRequest;
import com.nexus.portal.releaseorchestrator.dto.response.HostResponse;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.service.HostService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
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
@RestController("orchestratorHostController")
@RequestMapping("/api/v1/release-orchestrator/hosts")
public class HostController {

  private final HostService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.HOST_CRIAR)
  public HostResponse criar(@Valid @RequestBody HostRequest request) {
    return HostResponse.from(service.criar(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.HOST_EDITAR)
  public HostResponse atualizar(@PathVariable UUID id, @Valid @RequestBody HostRequest request) {
    return HostResponse.from(service.atualizar(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize(Permissoes.HOST_EDITAR)
  public HostResponse alterarStatus(@PathVariable UUID id,
      @Valid @RequestBody AlterarStatusHostRequest request) {
    return HostResponse.from(service.alterarStatus(id, request.ativo()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.HOST_EXCLUIR)
  public void excluir(@PathVariable UUID id) {
    service.excluir(id);
  }

  @PreAuthorize(Permissoes.HOST_LER)
  @GetMapping("/{id}")
  public HostResponse buscar(@PathVariable UUID id) {
    return HostResponse.from(service.buscar(id));
  }

  @PreAuthorize(Permissoes.HOST_LER)
  @GetMapping
  public PageResponse<HostResponse> listar(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean ativo,
      @RequestParam(required = false) SistemaOperacionalHost sistemaOperacional,
      @RequestParam(required = false) Boolean dockerDisponivel,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("codigo", "nome", "hostname", "sistemaOperacional", "ativo", "createdAt", "updatedAt"),
        Sort.by("nome"));
    return PageResponse.from(
        service.listar(q, ativo, sistemaOperacional, dockerDisponivel,
            PageableUtils.of(page, size, sortOrder)),
        HostResponse::from);
  }
}
