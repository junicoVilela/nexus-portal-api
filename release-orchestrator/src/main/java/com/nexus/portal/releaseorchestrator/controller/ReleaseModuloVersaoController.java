package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.SalvarVersaoModuloRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ReleaseModuloVersaoResponse;
import com.nexus.portal.releaseorchestrator.service.ReleaseModuloVersaoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/releases/{releaseId}/modulos-versao")
public class ReleaseModuloVersaoController {

  private final ReleaseModuloVersaoService service;

  @PreAuthorize(Permissoes.RELEASE_LER)
  @GetMapping
  public List<ReleaseModuloVersaoResponse> listar(@PathVariable UUID releaseId) {
    return service.listar(releaseId).stream()
        .map(ReleaseModuloVersaoResponse::from)
        .toList();
  }

  @PutMapping("/{moduloProdutoId}")
  @PreAuthorize(Permissoes.RELEASE_EDITAR)
  public ReleaseModuloVersaoResponse salvar(@PathVariable UUID releaseId,
      @PathVariable UUID moduloProdutoId,
      @Valid @RequestBody SalvarVersaoModuloRequest request) {
    return ReleaseModuloVersaoResponse.from(
        service.salvar(releaseId, moduloProdutoId, request.versao()));
  }

  @DeleteMapping("/{moduloProdutoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.RELEASE_EDITAR)
  public void remover(@PathVariable UUID releaseId, @PathVariable UUID moduloProdutoId) {
    service.remover(releaseId, moduloProdutoId);
  }
}
