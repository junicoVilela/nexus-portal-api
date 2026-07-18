package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.ProjetoService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.Permissoes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import br.com.softon.portal.docflow.dto.request.ProjetoRequest;
import br.com.softon.portal.docflow.dto.response.ProjetoResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/docflow/projetos")
public class ProjetoController {
  private final ProjetoService projetoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PROJETO_CRIAR)
  public ProjetoResponse criar(@Valid @RequestBody ProjetoRequest request) {
    return ProjetoResponse.from(projetoService.criar(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.PROJETO_EDITAR)
  public ProjetoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ProjetoRequest request) {
    return ProjetoResponse.from(projetoService.atualizar(id, request));
  }

  @PreAuthorize(Permissoes.PROJETO_LER)
  @GetMapping
  public PageResponse<ProjetoResponse> listar(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("nome", "slug", "ativo", "createdAt", "updatedAt"),
        Sort.by("nome"));
    return PageResponse.from(
        projetoService.listar(nome, PageableUtils.of(page, size, sortOrder)),
        ProjetoResponse::from);
  }

  @PreAuthorize(Permissoes.PROJETO_LER)
  @GetMapping("/{id}")
  public ProjetoResponse buscar(@PathVariable UUID id) {
    return ProjetoResponse.from(projetoService.buscar(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PROJETO_EXCLUIR)
  public void excluir(@PathVariable UUID id, Principal principal) {
    projetoService.excluir(id, principal);
  }
}
