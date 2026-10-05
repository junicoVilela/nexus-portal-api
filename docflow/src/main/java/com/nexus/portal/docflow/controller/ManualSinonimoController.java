package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.entity.ManualSinonimo;
import com.nexus.portal.docflow.service.ManualSinonimoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sinônimos da busca do manual por cliente ("NF" acha "nota fiscal"). */
@RestController
public class ManualSinonimoController {

  private final ManualSinonimoService service;

  public ManualSinonimoController(ManualSinonimoService service) {
    this.service = service;
  }

  public record SinonimoRequest(@NotNull @Size(max = 20) List<@Size(max = 200) String> termos) {}

  public record SinonimoResponse(UUID id, UUID clienteId, List<String> termos, OffsetDateTime createdAt,
      String createdBy, OffsetDateTime updatedAt) {

    static SinonimoResponse from(ManualSinonimo s) {
      return new SinonimoResponse(s.getId(), s.getClienteId(), s.getTermos(), s.getCreatedAt(), s.getCreatedBy(),
          s.getUpdatedAt());
    }
  }

  @GetMapping("/api/v1/docflow/clientes/{clienteId}/sinonimos-manual")
  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  public List<SinonimoResponse> listar(@PathVariable UUID clienteId) {
    return service.listar(clienteId).stream().map(SinonimoResponse::from).toList();
  }

  @PostMapping("/api/v1/docflow/clientes/{clienteId}/sinonimos-manual")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public SinonimoResponse criar(@PathVariable UUID clienteId, @Valid @RequestBody SinonimoRequest request,
      Principal principal) {
    return SinonimoResponse.from(service.criar(clienteId, request.termos(), principal));
  }

  @PutMapping("/api/v1/docflow/sinonimos-manual/{id}")
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public SinonimoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody SinonimoRequest request) {
    return SinonimoResponse.from(service.atualizar(id, request.termos()));
  }

  @DeleteMapping("/api/v1/docflow/sinonimos-manual/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public void excluir(@PathVariable UUID id) {
    service.excluir(id);
  }
}
