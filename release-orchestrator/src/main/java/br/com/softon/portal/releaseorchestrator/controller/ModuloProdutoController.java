package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusModuloRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.AtualizarModuloProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.CriarModuloProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ModuloProdutoResponse;
import br.com.softon.portal.releaseorchestrator.service.ModuloProdutoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.SecurityRoles;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/produtos/{produtoId}/modulos")
@PreAuthorize(SecurityRoles.WRITE)
public class ModuloProdutoController {

  private final ModuloProdutoService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public List<ModuloProdutoResponse> listar(@PathVariable UUID produtoId) {
    return service.listar(produtoId).stream().map(ModuloProdutoResponse::from).toList();
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public ModuloProdutoResponse buscar(@PathVariable UUID produtoId, @PathVariable UUID id) {
    return ModuloProdutoResponse.from(service.buscar(produtoId, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ModuloProdutoResponse criar(@PathVariable UUID produtoId,
      @Valid @RequestBody CriarModuloProdutoRequest request) {
    return ModuloProdutoResponse.from(service.criar(produtoId, request));
  }

  @PutMapping("/{id}")
  public ModuloProdutoResponse atualizar(@PathVariable UUID produtoId, @PathVariable UUID id,
      @Valid @RequestBody AtualizarModuloProdutoRequest request) {
    return ModuloProdutoResponse.from(service.atualizar(produtoId, id, request));
  }

  @PatchMapping("/{id}/status")
  public ModuloProdutoResponse alterarStatus(@PathVariable UUID produtoId, @PathVariable UUID id,
      @Valid @RequestBody AlterarStatusModuloRequest request) {
    return ModuloProdutoResponse.from(service.alterarStatus(produtoId, id, request.ativo()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable UUID produtoId, @PathVariable UUID id) {
    service.excluir(produtoId, id);
  }
}
