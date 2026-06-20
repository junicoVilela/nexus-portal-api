package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusCatalogoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.DominioProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.DominioProdutoResponse;
import br.com.softon.portal.releaseorchestrator.service.DominioProdutoService;
import br.com.softon.portal.shared.security.SecurityRoles;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorDominioProdutoController")
@RequestMapping("/api/v1/release-orchestrator/produtos/{produtoId}/dominios")
@PreAuthorize(SecurityRoles.WRITE)
public class DominioProdutoController {

  private final DominioProdutoService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public List<DominioProdutoResponse> listar(@PathVariable UUID produtoId) {
    return service.listar(produtoId).stream().map(DominioProdutoResponse::from).toList();
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public DominioProdutoResponse buscar(@PathVariable UUID produtoId, @PathVariable UUID id) {
    return DominioProdutoResponse.from(service.buscar(produtoId, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DominioProdutoResponse criar(@PathVariable UUID produtoId,
      @Valid @RequestBody DominioProdutoRequest request) {
    return DominioProdutoResponse.from(service.criar(produtoId, request));
  }

  @PutMapping("/{id}")
  public DominioProdutoResponse atualizar(@PathVariable UUID produtoId, @PathVariable UUID id,
      @Valid @RequestBody DominioProdutoRequest request) {
    return DominioProdutoResponse.from(service.atualizar(produtoId, id, request));
  }

  @PatchMapping("/{id}/status")
  public DominioProdutoResponse alterarStatus(@PathVariable UUID produtoId, @PathVariable UUID id,
      @Valid @RequestBody AlterarStatusCatalogoRequest request) {
    return DominioProdutoResponse.from(service.alterarStatus(produtoId, id, request.ativo()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable UUID produtoId, @PathVariable UUID id) {
    service.excluir(produtoId, id);
  }
}
