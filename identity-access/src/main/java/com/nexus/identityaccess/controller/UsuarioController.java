package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.dto.request.AlterarSenhaRequest;
import com.nexus.identityaccess.dto.request.AtualizarUsuarioRequest;
import com.nexus.identityaccess.dto.request.BloqueioUsuarioRequest;
import com.nexus.identityaccess.dto.request.CriarUsuarioRequest;
import com.nexus.identityaccess.dto.request.UsuarioGruposRequest;
import com.nexus.identityaccess.dto.response.UsuarioResponse;
import com.nexus.identityaccess.service.UsuarioService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
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

@RestController
@RequestMapping("/api/v1/rbac/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;

  @GetMapping
  @PreAuthorize(Permissoes.USUARIO_LER)
  public PageResponse<UsuarioResponse> listar(
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("username", "nome", "email", "ativo", "createdAt", "updatedAt"),
        Sort.by("username"));
    return PageResponse.from(
        usuarioService.listar(PageableUtils.of(page, size, sortOrder)),
        UsuarioResponse::from);
  }

  @GetMapping("/{id}")
  @PreAuthorize(Permissoes.USUARIO_LER)
  public UsuarioResponse buscar(@PathVariable UUID id) {
    return UsuarioResponse.from(usuarioService.buscar(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.USUARIO_CRIAR)
  public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request, Principal principal) {
    return UsuarioResponse.from(usuarioService.criar(request.username(), request.password(),
        request.nome(), request.email(), principal));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public UsuarioResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody AtualizarUsuarioRequest request, Principal principal) {
    return UsuarioResponse.from(usuarioService.atualizar(id, request.nome(), request.email(),
        request.ativo(), principal));
  }

  @PostMapping("/{id}/alterar-senha")
  @PreAuthorize(Permissoes.USUARIO_RESETAR)
  public void alterarSenha(@PathVariable UUID id, @Valid @RequestBody AlterarSenhaRequest request, Principal principal) {
    usuarioService.alterarSenha(id, request.novaSenha(), principal);
  }

  @GetMapping("/{id}/grupos")
  @PreAuthorize(Permissoes.USUARIO_LER)
  public List<UUID> listarGrupos(@PathVariable UUID id) {
    return usuarioService.listarGrupos(id);
  }

  @PutMapping("/{id}/grupos")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public List<UUID> salvarGrupos(@PathVariable UUID id,
      @Valid @RequestBody UsuarioGruposRequest request, Principal principal) {
    return usuarioService.salvarGrupos(id, request.grupoIds(), principal);
  }

  @PostMapping("/{id}/bloqueio")
  @PreAuthorize(Permissoes.USUARIO_BLOQUEAR)
  public UsuarioResponse alterarBloqueio(@PathVariable UUID id,
      @Valid @RequestBody BloqueioUsuarioRequest request, Principal principal) {
    return UsuarioResponse.from(
        usuarioService.alterarBloqueio(id, request.bloqueado(), principal));
  }
}
