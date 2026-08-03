package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.dto.request.AlterarStatusGrupoRequest;
import com.nexus.identityaccess.dto.request.GrupoPermissoesRequest;
import com.nexus.identityaccess.dto.request.GrupoRequest;
import com.nexus.identityaccess.dto.request.GrupoUsuariosRequest;
import com.nexus.identityaccess.dto.response.GrupoResponse;
import com.nexus.identityaccess.entity.Grupo;
import com.nexus.identityaccess.service.GrupoService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/grupos")
public class GrupoController {

  private final GrupoService grupoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.GRUPO_ACESSO_CRIAR)
  public GrupoResponse criar(@Valid @RequestBody GrupoRequest request, Principal principal) {
    return GrupoResponse.from(grupoService.criar(request, principal), List.of());
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_EDITAR)
  public GrupoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody GrupoRequest request, Principal principal) {
    return GrupoResponse.from(grupoService.atualizar(id, request, principal), List.of());
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_EDITAR)
  public GrupoResponse alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusGrupoRequest request, Principal principal) {
    return GrupoResponse.from(grupoService.alterarStatus(id, request.ativo(), principal), List.of());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.GRUPO_ACESSO_EXCLUIR)
  public void excluir(@PathVariable UUID id, Principal principal) {
    grupoService.excluir(id, principal);
  }

  @GetMapping
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public PageResponse<GrupoResponse> listar(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("nome", "ativo", "createdAt", "updatedAt"),
        Sort.by("nome"));
    return PageResponse.from(
        grupoService.listar(nome, PageableUtils.of(page, size, sortOrder)),
        g -> GrupoResponse.from(g, List.of()));
  }

  @GetMapping("/{id}")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public GrupoResponse buscar(@PathVariable UUID id) {
    Grupo grupo = grupoService.buscar(id);
    return GrupoResponse.from(grupo, grupoService.listarPermissoes(id));
  }

  @GetMapping("/{id}/usuarios")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public List<UUID> listarMembros(@PathVariable UUID id) {
    return grupoService.listarMembros(id);
  }

  @PutMapping("/{id}/usuarios")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.GRUPO_ACESSO_EDITAR)
  public void salvarMembros(@PathVariable UUID id, @Valid @RequestBody GrupoUsuariosRequest request, Principal principal) {
    grupoService.salvarMembros(id, request, principal);
  }

  @GetMapping("/{id}/permissoes")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public List<String> listarPermissoes(@PathVariable UUID id) {
    return grupoService.listarPermissoes(id);
  }

  @PutMapping("/{id}/permissoes")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.GRUPO_ACESSO_VINCULAR_PERMISSAO)
  public void salvarPermissoes(@PathVariable UUID id, @Valid @RequestBody GrupoPermissoesRequest request, Principal principal) {
    grupoService.salvarPermissoes(id, request, principal);
  }
}
