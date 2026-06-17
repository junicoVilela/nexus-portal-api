package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.PreviewTokenService;
import br.com.softon.portal.docflow.entity.PreviewToken;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.softon.portal.docflow.dto.response.PreviewTokenResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class PreviewController {

  private final PreviewTokenService previewTokenService;

  @PostMapping("/api/v1/preview-tokens")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public PreviewTokenResponse gerarToken(
      @RequestParam UUID clienteId,
      @RequestParam(defaultValue = "72") int horasValidade,
      Principal principal) {
    PreviewToken token = previewTokenService.gerar(clienteId, horasValidade, principal);
    return PreviewTokenResponse.from(token);
  }

  @GetMapping("/api/v1/preview-tokens")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public List<PreviewTokenResponse> listar(@RequestParam UUID clienteId) {
    return previewTokenService.listar(clienteId).stream().map(PreviewTokenResponse::from).toList();
  }

  @DeleteMapping("/api/v1/preview-tokens/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public void revogar(@PathVariable UUID id, Principal principal) {
    previewTokenService.revogar(id, principal);
  }

  @GetMapping(value = "/api/v1/preview/{token}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> preview(@PathVariable String token) {
    String html = previewTokenService.renderizarPreview(token);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(html);
  }
}
