package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.EmpresaLogoService;
import br.com.softon.portal.shared.security.Permissoes;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/docflow/empresa")
public class EmpresaController {

  private final EmpresaLogoService empresaLogoService;

  @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.EMPRESA_EDITAR)
  public void uploadLogo(@RequestParam MultipartFile file) {
    empresaLogoService.salvarLogo(file);
  }

  @GetMapping("/logo")
  public ResponseEntity<Resource> getLogo() {
    return empresaLogoService.servir();
  }

  @DeleteMapping("/logo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.EMPRESA_EDITAR)
  public void deleteLogo() {
    empresaLogoService.removerLogo();
  }
}
