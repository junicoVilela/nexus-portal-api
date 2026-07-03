package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusCatalogoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.FuncionalidadeProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.FuncionalidadeProdutoResponse;
import br.com.softon.portal.releaseorchestrator.service.FuncionalidadeProdutoService;
import br.com.softon.portal.shared.security.Permissoes;
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
@RestController("orchestratorFuncionalidadeProdutoController")
@RequestMapping("/api/v1/release-orchestrator/produtos/{produtoId}/dominios/{dominioId}/funcionalidades")
public class FuncionalidadeProdutoController {

  private final FuncionalidadeProdutoService service;

  @PreAuthorize(Permissoes.PRODUTO_LER)
  @GetMapping
  public List<FuncionalidadeProdutoResponse> listar(@PathVariable UUID produtoId,
      @PathVariable UUID dominioId) {
    return service.listar(produtoId, dominioId).stream()
        .map(FuncionalidadeProdutoResponse::from).toList();
  }

  @PreAuthorize(Permissoes.PRODUTO_LER)
  @GetMapping("/{id}")
  public FuncionalidadeProdutoResponse buscar(@PathVariable UUID produtoId,
      @PathVariable UUID dominioId, @PathVariable UUID id) {
    return FuncionalidadeProdutoResponse.from(service.buscar(produtoId, dominioId, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PRODUTO_EDITAR)
  public FuncionalidadeProdutoResponse criar(@PathVariable UUID produtoId,
      @PathVariable UUID dominioId,
      @Valid @RequestBody FuncionalidadeProdutoRequest request) {
    return FuncionalidadeProdutoResponse.from(service.criar(produtoId, dominioId, request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.PRODUTO_EDITAR)
  public FuncionalidadeProdutoResponse atualizar(@PathVariable UUID produtoId,
      @PathVariable UUID dominioId, @PathVariable UUID id,
      @Valid @RequestBody FuncionalidadeProdutoRequest request) {
    return FuncionalidadeProdutoResponse.from(
        service.atualizar(produtoId, dominioId, id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize(Permissoes.PRODUTO_EDITAR)
  public FuncionalidadeProdutoResponse alterarStatus(@PathVariable UUID produtoId,
      @PathVariable UUID dominioId, @PathVariable UUID id,
      @Valid @RequestBody AlterarStatusCatalogoRequest request) {
    return FuncionalidadeProdutoResponse.from(
        service.alterarStatus(produtoId, dominioId, id, request.ativo()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PRODUTO_EDITAR)
  public void excluir(@PathVariable UUID produtoId, @PathVariable UUID dominioId,
      @PathVariable UUID id) {
    service.excluir(produtoId, dominioId, id);
  }
}
