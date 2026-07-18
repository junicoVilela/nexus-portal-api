package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.dto.request.PublicacaoRequest;
import br.com.softon.portal.docflow.dto.response.ChangelogItemResponse;
import br.com.softon.portal.docflow.dto.response.DownloadTokenResponse;
import br.com.softon.portal.docflow.dto.response.PaginaResponse;
import br.com.softon.portal.docflow.dto.response.PublicacaoResponse;
import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.docflow.service.GeradorPdfService;
import br.com.softon.portal.docflow.service.PublicacaoService;
import br.com.softon.portal.docflow.service.PublicacaoEventService;
import br.com.softon.portal.docflow.service.PublicacaoService.DiagnosticoPublicacao;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import br.com.softon.portal.shared.config.JwtService;
import br.com.softon.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/docflow/publicacoes")
public class PublicacaoController {

  private final PublicacaoService publicacaoService;
  private final PublicacaoEventService publicacaoEventService;
  private final GeradorPdfService geradorPdfService;
  private final JwtService jwtService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PUBLICACAO_CRIAR)
  public PublicacaoResponse gerar(@Valid @RequestBody PublicacaoRequest request, Principal principal) {
    return PublicacaoResponse.from(publicacaoService.gerar(request.clienteId(), request.versao(),
        request.observacao(), principal));
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping
  public PageResponse<PublicacaoResponse> listar(
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("createdAt", "cliente.nome", "versao", "status", "updatedAt"),
        Sort.by(Sort.Order.desc("createdAt")));
    return PageResponse.from(
        publicacaoService.listar(clienteId, PageableUtils.of(page, size, sortOrder)),
        PublicacaoResponse::from);
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter eventos() {
    return publicacaoEventService.inscrever();
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping("/{id}")
  public PublicacaoResponse buscar(@PathVariable UUID id) {
    return PublicacaoResponse.from(publicacaoService.buscar(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PUBLICACAO_EXCLUIR)
  public void excluir(@PathVariable UUID id, Principal principal) {
    publicacaoService.excluir(id, principal);
  }

  @PostMapping("/{id}/reprocessar")
  @PreAuthorize(Permissoes.PUBLICACAO_EDITAR)
  public PublicacaoResponse reprocessar(@PathVariable UUID id, Principal principal) {
    return PublicacaoResponse.from(publicacaoService.reprocessar(id, principal));
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping("/preview")
  public List<PaginaResponse> preview(@RequestParam UUID clienteId) {
    return publicacaoService.preverPaginas(clienteId);
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping(value = "/preview-html", produces = MediaType.TEXT_HTML_VALUE)
  public String previewHtml(@RequestParam UUID clienteId, @RequestParam(required = false) String versao) {
    return publicacaoService.previewHtml(clienteId, versao);
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping("/diagnostico")
  public List<DiagnosticoPublicacao> diagnostico(@RequestParam UUID clienteId) {
    return publicacaoService.diagnosticar(clienteId);
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable UUID id) {
    Publicacao publicacao = publicacaoService.buscar(id);
    Resource resource = publicacaoService.recursoPacoteDownloadPublico(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + publicacao.getArquivoZipNome() + "\"")
        .body(resource);
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping("/{id}/download-token")
  public DownloadTokenResponse emitirTokenDownload(@PathVariable UUID id) {
    publicacaoService.recursoPacoteDownloadPublico(id);
    String token = jwtService.gerarTokenDownloadPacote(id);
    return new DownloadTokenResponse(token, jwtService.downloadTtlMillis() / 1000,
        "/api/v1/public/publicacoes/download");
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<Resource> downloadPdf(@PathVariable UUID id) {
    Publicacao publicacao = publicacaoService.buscar(id);
    String html = publicacaoService.previewHtml(publicacao.getCliente().getId(),
        publicacao.getVersao());
    byte[] pdf = geradorPdfService.gerarPdf(html);
    String filename = "manual-" + publicacao.getCliente().getNome().replaceAll("[^a-zA-Z0-9]", "-")
        + "-v" + publicacao.getVersao() + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(new ByteArrayResource(pdf));
  }

  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  @GetMapping("/{id}/changelog")
  public List<ChangelogItemResponse> changelog(@PathVariable UUID id) {
    return publicacaoService.listarChangelog(id).stream()
        .map(c -> new ChangelogItemResponse(c.getId(), c.getPaginaId(), c.getPaginaTitulo(),
            c.getTipoMudanca(), c.getCreatedAt()))
        .toList();
  }
}
