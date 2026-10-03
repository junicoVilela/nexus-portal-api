package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.request.AiAjustePaginaRequest;
import com.nexus.portal.ai.dto.response.AiAjustePaginaResponse;
import com.nexus.portal.ai.service.AiAjustePaginaService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/paginas")
public class AiAjustePaginaController {

  private final AiAjustePaginaService ajustePaginaService;

  public AiAjustePaginaController(AiAjustePaginaService ajustePaginaService) {
    this.ajustePaginaService = ajustePaginaService;
  }

  @PostMapping("/{paginaId}/ajustes")
  @PreAuthorize(Permissoes.PAGINA_AI_GERAR + " and " + Permissoes.PAGINA_EDITAR)
  public ResponseEntity<AiAjustePaginaResponse> pedir(
      @PathVariable UUID paginaId,
      @Valid @RequestBody AiAjustePaginaRequest request,
      Principal principal) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ajustePaginaService.pedir(paginaId, request, principal));
  }
}
