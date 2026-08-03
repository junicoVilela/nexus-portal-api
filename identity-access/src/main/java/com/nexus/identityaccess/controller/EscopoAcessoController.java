package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.dto.request.EscopoAcessoRequest;
import com.nexus.identityaccess.dto.response.EscopoAcessoResponse;
import com.nexus.identityaccess.service.EscopoAcessoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/v1/rbac/escopos")
public class EscopoAcessoController {

  private final EscopoAcessoService service;

  @GetMapping
  @PreAuthorize(Permissoes.USUARIO_LER)
  public List<EscopoAcessoResponse> listar(
      @RequestParam(required = false) UUID usuarioId,
      @RequestParam(required = false) UUID grupoId) {
    if (usuarioId != null) return service.listarPorUsuario(usuarioId).stream().map(EscopoAcessoResponse::from).toList();
    if (grupoId != null) return service.listarPorGrupo(grupoId).stream().map(EscopoAcessoResponse::from).toList();
    return service.listarTodos().stream().map(EscopoAcessoResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public EscopoAcessoResponse criar(@Valid @RequestBody EscopoAcessoRequest r, Principal principal) {
    return EscopoAcessoResponse.from(service.criar(r.usuarioId(), r.grupoAcessoId(),
        r.clienteId(), r.ambienteId(), r.produtoId(), r.tipoAmbiente(),
        r.somenteLeitura(), r.ativo(), principal));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public EscopoAcessoResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody EscopoAcessoRequest r, Principal principal) {
    return EscopoAcessoResponse.from(service.atualizar(id, r.clienteId(), r.ambienteId(),
        r.produtoId(), r.tipoAmbiente(), r.somenteLeitura(), r.ativo(), principal));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public EscopoAcessoResponse alterarStatus(@PathVariable UUID id,
      @RequestParam boolean ativo, Principal principal) {
    return EscopoAcessoResponse.from(service.alterarStatus(id, ativo, principal));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public void remover(@PathVariable UUID id, Principal principal) {
    service.remover(id, principal);
  }
}
