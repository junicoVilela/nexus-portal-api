package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.request.AiMensagemRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.request.CriarAiSessaoRequest;
import com.nexus.portal.ai.dto.response.AiAplicacaoResponse;
import com.nexus.portal.ai.dto.response.AiJobResponse;
import com.nexus.portal.ai.dto.response.AiPropostaResponse;
import com.nexus.portal.ai.dto.response.AiSessaoResponse;
import com.nexus.portal.ai.service.AiPropostaService;
import com.nexus.portal.ai.service.AiSessaoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/sessoes")
public class AiSessaoController {

  private final AiSessaoService aiSessaoService;
  private final AiPropostaService aiPropostaService;

  public AiSessaoController(AiSessaoService aiSessaoService, AiPropostaService aiPropostaService) {
    this.aiSessaoService = aiSessaoService;
    this.aiPropostaService = aiPropostaService;
  }

  @PostMapping
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiSessaoResponse criar(@Valid @RequestBody CriarAiSessaoRequest request, Principal principal) {
    return aiSessaoService.criar(request, principal);
  }

  @GetMapping("/{id}")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public AiSessaoResponse buscar(@PathVariable UUID id) {
    return aiSessaoService.buscar(id);
  }

  @PostMapping("/{id}/mensagens")
  @PreAuthorize("hasAuthority('PAGINA:CRIAR') or hasAuthority('PAGINA:EDITAR')")
  public AiSessaoResponse enviarMensagem(
      @PathVariable UUID id,
      @Valid @RequestBody AiMensagemRequest request) {
    return aiSessaoService.enviarMensagem(id, request);
  }

  @PostMapping("/{id}/cancelar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public AiSessaoResponse cancelar(@PathVariable UUID id, Principal principal) {
    return aiSessaoService.cancelar(id, principal);
  }

  @PostMapping("/{id}/gerar")
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public ResponseEntity<AiJobResponse> gerar(@PathVariable UUID id, Principal principal) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(aiPropostaService.gerar(id, principal));
  }

  @GetMapping("/{id}/proposta")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public AiPropostaResponse proposta(@PathVariable UUID id) {
    return aiPropostaService.propostaAtual(id);
  }

  @PostMapping("/{id}/aplicar")
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiAplicacaoResponse aplicar(
      @PathVariable UUID id,
      @Valid @RequestBody AplicarAiPropostaRequest request,
      Principal principal) {
    return aiPropostaService.aplicar(id, request, principal);
  }
}

