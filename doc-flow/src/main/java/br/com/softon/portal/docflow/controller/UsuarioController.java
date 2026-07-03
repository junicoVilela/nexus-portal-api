package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.dto.request.AlterarSenhaRequest;
import br.com.softon.portal.docflow.dto.request.AtualizarUsuarioRequest;
import br.com.softon.portal.docflow.dto.request.CriarUsuarioRequest;
import br.com.softon.portal.docflow.dto.response.UsuarioResponse;
import br.com.softon.portal.docflow.service.UsuarioService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import br.com.softon.portal.shared.security.Permissoes;
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

@RestController
@RequestMapping("/api/v1/docflow/usuarios")
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
  public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
    return UsuarioResponse.from(usuarioService.criar(request.username(), request.password(),
        request.nome(), request.email(), request.roles()));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public UsuarioResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody AtualizarUsuarioRequest request) {
    return UsuarioResponse.from(usuarioService.atualizar(id, request.nome(), request.email(),
        request.roles(), request.ativo()));
  }

  @PostMapping("/{id}/alterar-senha")
  @PreAuthorize(Permissoes.USUARIO_RESETAR)
  public void alterarSenha(@PathVariable UUID id, @Valid @RequestBody AlterarSenhaRequest request) {
    usuarioService.alterarSenha(id, request.novaSenha());
  }
}
