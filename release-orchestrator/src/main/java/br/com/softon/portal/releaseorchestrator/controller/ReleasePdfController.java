package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.entity.TipoPdfRelease;
import br.com.softon.portal.releaseorchestrator.service.ReleasePdfService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.SecurityRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/releases/{releaseId}/pdf")
@PreAuthorize(SecurityRoles.WRITE)
public class ReleasePdfController {

  private final ReleasePdfService service;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public ResponseEntity<Resource> gerar(@PathVariable UUID releaseId,
      @RequestParam(defaultValue = "CLIENTE") TipoPdfRelease tipo) {
    byte[] pdf = service.gerar(releaseId, tipo);
    String nomeArquivo = "release-" + releaseId + "-" + tipo.name().toLowerCase() + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.length)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + nomeArquivo + "\"")
        .body(new ByteArrayResource(pdf));
  }
}
