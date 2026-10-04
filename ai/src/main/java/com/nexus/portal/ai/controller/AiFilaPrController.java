package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.request.AiFilaAceitarRequest;
import com.nexus.portal.ai.dto.request.RejeitarAiPropostaRequest;
import com.nexus.portal.ai.dto.response.AiAplicacaoResponse;
import com.nexus.portal.ai.dto.response.AiFilaPrItemResponse;
import com.nexus.portal.ai.service.AiFilaPrService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Fila de propostas abertas a partir de PRs mergeados (Fase C). */
@RestController
@RequestMapping("/api/v1/ai/fila-pr")
public class AiFilaPrController {

  private final AiFilaPrService service;

  public AiFilaPrController(AiFilaPrService service) {
    this.service = service;
  }

  /** {@code pendentes=true}: só o que precisa de alguém. */
  @GetMapping
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA)
  public List<AiFilaPrItemResponse> listar(@RequestParam(defaultValue = "true") boolean pendentes) {
    return service.listar(pendentes);
  }

  @GetMapping("/{id}")
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA)
  public AiFilaPrItemResponse buscar(@PathVariable UUID id) {
    return service.buscar(id);
  }

  /** Antes de abrir no editor ou regenerar: a sessão passa a ser de quem assumiu. */
  @PostMapping("/{id}/assumir")
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA)
  public AiFilaPrItemResponse assumir(@PathVariable UUID id, Principal principal) {
    return service.assumir(id, principal);
  }

  @PostMapping("/{id}/rejeitar")
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA)
  public AiFilaPrItemResponse rejeitar(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) RejeitarAiPropostaRequest request,
      Principal principal) {
    return service.rejeitar(id, request, principal);
  }

  /** Página nova: cria o rascunho no DocFlow. */
  @PostMapping("/{id}/aceitar")
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA + " and " + Permissoes.PAGINA_AI_APLICAR + " and "
      + Permissoes.PAGINA_CRIAR)
  public AiAplicacaoResponse aceitar(
      @PathVariable UUID id,
      @RequestBody(required = false) AiFilaAceitarRequest request,
      Principal principal) {
    return service.aceitar(id, request, principal);
  }

  /** "Revisado, nada a mudar": tira da fila sem gerar ajuste. */
  @PostMapping("/{id}/dispensar")
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA)
  public AiFilaPrItemResponse dispensar(@PathVariable UUID id, Principal principal) {
    return service.dispensar(id, principal);
  }

  /** Erro, página que voltou a rascunho ou item de release: processa (gera o ajuste) de novo. */
  @PostMapping("/{id}/reprocessar")
  @PreAuthorize(Permissoes.PAGINA_AI_PROPOSTA)
  public AiFilaPrItemResponse reprocessar(@PathVariable UUID id) {
    return service.reprocessar(id);
  }
}
