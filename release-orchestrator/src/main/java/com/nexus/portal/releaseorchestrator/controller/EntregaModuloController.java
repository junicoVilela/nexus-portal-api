package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarSelecaoModuloRequest;
import com.nexus.portal.releaseorchestrator.dto.response.EntregaModuloResponse;
import com.nexus.portal.releaseorchestrator.service.EntregaModuloService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorEntregaModuloController")
@RequestMapping("/api/v1/release-orchestrator/entregas/{entregaId}/modulos")
@Transactional
public class EntregaModuloController {

  private final EntregaModuloService service;

  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping
  public List<EntregaModuloResponse> listar(@PathVariable UUID entregaId) {
    return service.listar(entregaId).stream().map(EntregaModuloResponse::from).toList();
  }

  /**
   * Reconstrói a seleção a partir do contrato + release. Idempotente: pode
   * ser chamado várias vezes em RASCUNHO (limpa estado anterior).
   */
  @PostMapping("/inicializar")
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public List<EntregaModuloResponse> inicializar(@PathVariable UUID entregaId) {
    return service.inicializar(entregaId).stream()
        .map(EntregaModuloResponse::from).toList();
  }

  @PatchMapping("/{moduloProdutoId}/selecao")
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public EntregaModuloResponse alterarSelecao(@PathVariable UUID entregaId,
      @PathVariable UUID moduloProdutoId,
      @Valid @RequestBody AlterarSelecaoModuloRequest request) {
    return EntregaModuloResponse.from(
        service.alterarSelecao(entregaId, moduloProdutoId, request.selecionado()));
  }
}
