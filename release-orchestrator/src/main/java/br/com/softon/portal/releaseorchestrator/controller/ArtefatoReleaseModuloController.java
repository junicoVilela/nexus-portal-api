package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.response.ArtefatoReleaseModuloResponse;
import br.com.softon.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import br.com.softon.portal.releaseorchestrator.service.ArtefatoReleaseModuloService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.Permissoes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/releases/{releaseId}/modulos/{moduloId}/artefatos")
public class ArtefatoReleaseModuloController {

  private final ArtefatoReleaseModuloService service;

  @PreAuthorize(Permissoes.RELEASE_LER)
  @GetMapping
  public List<ArtefatoReleaseModuloResponse> listar(@PathVariable UUID releaseId,
      @PathVariable UUID moduloId) {
    return service.listar(releaseId, moduloId).stream()
        .map(ArtefatoReleaseModuloResponse::from)
        .toList();
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.RELEASE_EDITAR)
  public ArtefatoReleaseModuloResponse upload(@PathVariable UUID releaseId,
      @PathVariable UUID moduloId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "observacao", required = false) String observacao) {
    return ArtefatoReleaseModuloResponse.from(
        service.upload(releaseId, moduloId, file, observacao));
  }

  @PreAuthorize(Permissoes.RELEASE_LER)
  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable UUID releaseId,
      @PathVariable UUID moduloId, @PathVariable UUID id) {
    ArtefatoReleaseModulo artefato = service.buscar(releaseId, moduloId, id);
    Resource resource = service.download(releaseId, moduloId, id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + artefato.getNomeArquivo() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(resource);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.RELEASE_EDITAR)
  public void excluir(@PathVariable UUID releaseId, @PathVariable UUID moduloId,
      @PathVariable UUID id) {
    service.excluir(releaseId, moduloId, id);
  }
}
