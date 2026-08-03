package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.service.PreviewTokenService;
import com.nexus.portal.docflow.entity.PreviewToken;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.nexus.portal.shared.security.Permissoes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.nexus.portal.docflow.dto.response.PreviewTokenResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class PreviewController {

  private final PreviewTokenService previewTokenService;

  @PostMapping("/api/v1/preview-tokens")
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public PreviewTokenResponse gerarToken(
      @RequestParam UUID clienteId,
      @RequestParam(defaultValue = "72") int horasValidade,
      Principal principal) {
    PreviewToken token = previewTokenService.gerar(clienteId, horasValidade, principal);
    return PreviewTokenResponse.from(token);
  }

  @GetMapping("/api/v1/preview-tokens")
  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  public List<PreviewTokenResponse> listar(@RequestParam UUID clienteId) {
    return previewTokenService.listar(clienteId).stream().map(PreviewTokenResponse::from).toList();
  }

  @DeleteMapping("/api/v1/preview-tokens/{id}")
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public void revogar(@PathVariable UUID id, Principal principal) {
    previewTokenService.revogar(id, principal);
  }

  // /api/v1/preview/{token} é público (permitAll em SecurityConfig).
  @GetMapping(value = "/api/v1/preview/{token}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> preview(@PathVariable String token) {
    String html = previewTokenService.renderizarPreview(token);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(html);
  }
}
