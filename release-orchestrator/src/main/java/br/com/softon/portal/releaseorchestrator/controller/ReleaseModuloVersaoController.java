package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.SalvarVersaoModuloRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ReleaseModuloVersaoResponse;
import br.com.softon.portal.releaseorchestrator.service.ReleaseModuloVersaoService;
import br.com.softon.portal.shared.security.SecurityRoles;
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
@PreAuthorize(SecurityRoles.WRITE)
public class ReleaseModuloVersaoController {

  private final ReleaseModuloVersaoService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public List<ReleaseModuloVersaoResponse> listar(@PathVariable UUID releaseId) {
    return service.listar(releaseId).stream()
        .map(ReleaseModuloVersaoResponse::from)
        .toList();
  }

  @PutMapping("/{moduloProdutoId}")
  public ReleaseModuloVersaoResponse salvar(@PathVariable UUID releaseId,
      @PathVariable UUID moduloProdutoId,
      @Valid @RequestBody SalvarVersaoModuloRequest request) {
    return ReleaseModuloVersaoResponse.from(
        service.salvar(releaseId, moduloProdutoId, request.versao()));
  }

  @DeleteMapping("/{moduloProdutoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remover(@PathVariable UUID releaseId, @PathVariable UUID moduloProdutoId) {
    service.remover(releaseId, moduloProdutoId);
  }
}
