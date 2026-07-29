package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.service.DocumentoEntregaService;
import br.com.softon.portal.releaseorchestrator.service.EntregaService;
import br.com.softon.portal.shared.security.Permissoes;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorDocumentoEntregaController")
@RequestMapping("/api/v1/release-orchestrator/entregas/{entregaId}/documento")
@Transactional
public class DocumentoEntregaController {

  private final DocumentoEntregaService service;
  private final EntregaService entregaService;

  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping
  public ResponseEntity<Resource> gerar(@PathVariable UUID entregaId) {
    byte[] pdf = service.gerar(entregaId);
    var entrega = entregaService.buscar(entregaId);
    String nome = "documento-" + entrega.getCliente().getSigla().toLowerCase()
        + "-" + entrega.getProduto().getSigla().toLowerCase()
        + "-" + entrega.getRelease().getVersao() + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.length)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + nome + "\"")
        .body(new ByteArrayResource(pdf));
  }
}
