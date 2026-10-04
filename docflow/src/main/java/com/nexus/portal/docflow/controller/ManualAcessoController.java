package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.entity.ManualAcesso;
import com.nexus.portal.docflow.service.ManualAcessoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Chaves de integração do manual por cliente (Onda D, INT-403/405). */
@RestController
public class ManualAcessoController {

  private final ManualAcessoService service;

  public ManualAcessoController(ManualAcessoService service) {
    this.service = service;
  }

  public record CriarRequest(
      @NotBlank @Size(max = 120) String nome,
      @Size(max = 20) List<@NotBlank @Size(max = 200) String> origens,
      /** Vazio ou 0 = não expira (revogue quando não precisar mais). */
      @Max(3650) Integer diasValidade) {}

  public record AcessoResponse(UUID id, UUID clienteId, String nome, String prefixo, List<String> origens,
      boolean ativo, OffsetDateTime expiraEm, OffsetDateTime ultimoUsoEm, OffsetDateTime createdAt, String createdBy) {

    static AcessoResponse from(ManualAcesso a) {
      return new AcessoResponse(a.getId(), a.getClienteId(), a.getNome(), a.getPrefixo(), a.getOrigens(),
          a.valido(OffsetDateTime.now()), a.getExpiraEm(), a.getUltimoUsoEm(), a.getCreatedAt(), a.getCreatedBy());
    }
  }

  /** {@code token} só vem nesta resposta: guarde-o; depois só o prefixo aparece. */
  public record CriadoResponse(AcessoResponse acesso, String token) {}

  @GetMapping("/api/v1/docflow/clientes/{clienteId}/acessos-manual")
  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  public List<AcessoResponse> listar(@PathVariable UUID clienteId) {
    return service.listar(clienteId).stream().map(AcessoResponse::from).toList();
  }

  @PostMapping("/api/v1/docflow/clientes/{clienteId}/acessos-manual")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public CriadoResponse criar(@PathVariable UUID clienteId, @Valid @RequestBody CriarRequest request,
      Principal principal) {
    var criada = service.criar(clienteId, request.nome(), request.origens(), request.diasValidade(), principal);
    return new CriadoResponse(AcessoResponse.from(criada.acesso()), criada.token());
  }

  @DeleteMapping("/api/v1/docflow/acessos-manual/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public void revogar(@PathVariable UUID id, Principal principal) {
    service.revogar(id, principal);
  }
}
