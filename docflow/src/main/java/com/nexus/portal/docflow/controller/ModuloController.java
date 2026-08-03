package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.service.ModuloService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.nexus.portal.shared.security.Permissoes;
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
import com.nexus.portal.docflow.dto.request.ModuloRequest;
import com.nexus.portal.docflow.dto.response.ModuloResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/docflow/modulos")
public class ModuloController {
  private final ModuloService moduloService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.MODULO_CRIAR)
  public ModuloResponse criar(@Valid @RequestBody ModuloRequest request) {
    return ModuloResponse.from(moduloService.criar(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.MODULO_EDITAR)
  public ModuloResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ModuloRequest request) {
    return ModuloResponse.from(moduloService.atualizar(id, request));
  }

  @PreAuthorize(Permissoes.MODULO_LER)
  @GetMapping
  public PageResponse<ModuloResponse> listar(
      @RequestParam(required = false) UUID projetoId,
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("projeto.nome", "ordem", "nome", "slug", "ativo", "createdAt", "updatedAt"),
        Sort.by(Sort.Order.asc("projeto.nome"), Sort.Order.asc("ordem"), Sort.Order.asc("nome")));
    return PageResponse.from(
        moduloService.listar(
            projetoId,
            nome,
            PageableUtils.of(page, size, sortOrder)),
        ModuloResponse::from);
  }

  @PreAuthorize(Permissoes.MODULO_LER)
  @GetMapping("/{id}")
  public ModuloResponse buscar(@PathVariable UUID id) {
    return ModuloResponse.from(moduloService.buscar(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.MODULO_EXCLUIR)
  public void excluir(@PathVariable UUID id, Principal principal) {
    moduloService.excluir(id, principal);
  }
}
