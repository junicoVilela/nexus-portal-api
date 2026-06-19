package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.dto.request.AlterarStatusGrupoRequest;
import br.com.softon.portal.docflow.dto.request.GrupoPermissoesRequest;
import br.com.softon.portal.docflow.dto.request.GrupoRequest;
import br.com.softon.portal.docflow.dto.request.GrupoUsuariosRequest;
import br.com.softon.portal.docflow.dto.response.GrupoResponse;
import br.com.softon.portal.docflow.entity.Grupo;
import br.com.softon.portal.docflow.service.GrupoService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/docflow/grupos")
@PreAuthorize("hasRole('ADMIN')")
public class GrupoController {

  private final GrupoService grupoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public GrupoResponse criar(@Valid @RequestBody GrupoRequest request) {
    return GrupoResponse.from(grupoService.criar(request), List.of());
  }

  @PutMapping("/{id}")
  public GrupoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody GrupoRequest request) {
    return GrupoResponse.from(grupoService.atualizar(id, request), List.of());
  }

  @PatchMapping("/{id}/status")
  public GrupoResponse alterarStatus(@PathVariable UUID id, @Valid @RequestBody AlterarStatusGrupoRequest request) {
    return GrupoResponse.from(grupoService.alterarStatus(id, request.ativo()), List.of());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable UUID id) {
    grupoService.excluir(id);
  }

  @GetMapping
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
  public GrupoResponse buscar(@PathVariable UUID id) {
    Grupo grupo = grupoService.buscar(id);
    return GrupoResponse.from(grupo, grupoService.listarPermissoes(id));
  }

  @GetMapping("/{id}/usuarios")
  public List<UUID> listarMembros(@PathVariable UUID id) {
    return grupoService.listarMembros(id);
  }

  @PutMapping("/{id}/usuarios")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void salvarMembros(@PathVariable UUID id, @Valid @RequestBody GrupoUsuariosRequest request) {
    grupoService.salvarMembros(id, request);
  }

  @GetMapping("/{id}/permissoes")
  public List<String> listarPermissoes(@PathVariable UUID id) {
    return grupoService.listarPermissoes(id);
  }

  @PutMapping("/{id}/permissoes")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void salvarPermissoes(@PathVariable UUID id, @Valid @RequestBody GrupoPermissoesRequest request) {
    grupoService.salvarPermissoes(id, request);
  }
}
