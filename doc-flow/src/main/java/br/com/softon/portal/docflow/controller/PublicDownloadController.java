package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.PublicacaoService;
import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.shared.config.JwtService;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public")
public class PublicDownloadController {

  private final JwtService jwtService;
  private final PublicacaoService publicacaoService;

  @GetMapping("/publicacoes/download")
  public ResponseEntity<Resource> downloadZip(@RequestParam("token") String token) {
    UUID publicacaoId = jwtService.parseTokenDownloadPacoteValido(token.trim());
    Resource resource = publicacaoService.recursoPacoteDownloadPublico(publicacaoId);
    Publicacao publicacao = publicacaoService.buscar(publicacaoId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + publicacao.getArquivoZipNome() + "\"")
        .body(resource);
  }
}
