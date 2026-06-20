package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.PaginaAnexoService;
import br.com.softon.portal.docflow.service.PaginaService;
import br.com.softon.portal.docflow.entity.StatusPagina;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.SecurityRoles;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import br.com.softon.portal.docflow.dto.request.PaginaRequest;
import br.com.softon.portal.docflow.dto.request.ReordenarRequest;
import br.com.softon.portal.docflow.dto.response.PaginaAnexoResponse;
import br.com.softon.portal.docflow.dto.response.PaginaResponse;
import br.com.softon.portal.docflow.dto.response.PaginaRevisaoResponse;

@RestController
@RequestMapping("/api/v1/docflow/paginas")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.WRITE)
public class PaginaController {

  private final PaginaService paginaService;
  private final PaginaAnexoService paginaAnexoService;

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/resumo-por-status")
  public Map<String, Long> resumoPorStatusGlobal() {
    Map<String, Long> map = new LinkedHashMap<>();
    paginaService.resumoContagemPorStatusGlobal().forEach((status, total) -> map.put(status.name(), total));
    return map;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaginaResponse criar(@Valid @RequestBody PaginaRequest request, Principal principal) {
    return PaginaResponse.from(paginaService.criar(request, principal));
  }

  @PutMapping("/{id}")
  public PaginaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PaginaRequest request,
      Principal principal) {
    return PaginaResponse.from(paginaService.atualizar(id, request, principal));
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping
  public PageResponse<PaginaResponse> listar(@RequestParam(required = false) String titulo,
      @RequestParam(required = false) UUID moduloId,
      @RequestParam(required = false) UUID projetoId,
      @RequestParam(required = false) StatusPagina status,
      @RequestParam(required = false) String codigoTela,
      @RequestParam(required = false) String busca,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("modulo.projeto.nome", "modulo.nome", "modulo.ordem", "parent.ordem", "ordem", "titulo", "codigoTela", "status", "createdAt", "updatedAt"),
        Sort.by(
            Sort.Order.asc("modulo.projeto.nome"),
            Sort.Order.asc("modulo.ordem"),
            Sort.Order.asc("parent.ordem").nullsFirst(),
            Sort.Order.asc("ordem"),
            Sort.Order.asc("titulo")));
    return PageResponse.from(
        paginaService.listar(
            titulo,
            moduloId,
            projetoId,
            status,
            codigoTela,
            busca,
            PageableUtils.of(page, size, sortOrder)),
        PaginaResponse::from);
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}")
  public PaginaResponse buscar(@PathVariable UUID id) {
    return PaginaResponse.from(paginaService.buscar(id));
  }

  @PostMapping("/{id}/salvar-rascunho")
  public PaginaResponse salvarRascunho(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.salvarRascunho(id, principal));
  }

  @PostMapping("/{id}/enviar-revisao")
  public PaginaResponse enviarRevisao(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.enviarRevisao(id, principal));
  }

  @PostMapping("/{id}/aprovar")
  public PaginaResponse aprovar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.aprovar(id, principal));
  }

  @PostMapping("/{id}/publicar")
  public PaginaResponse publicar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.publicar(id, principal));
  }

  @PostMapping("/{id}/arquivar")
  public PaginaResponse arquivar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.arquivar(id, principal));
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
  public String preview(@PathVariable UUID id) {
    return paginaService.preview(id);
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}/revisoes")
  public PageResponse<PaginaRevisaoResponse> revisoes(
      @PathVariable UUID id,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("numero", "createdAt", "status"),
        Sort.by(Sort.Order.desc("numero")));
    return PageResponse.from(
        paginaService.revisoes(id, PageableUtils.of(page, size, sortOrder)),
        PaginaRevisaoResponse::from);
  }

  @PostMapping("/{id}/duplicar")
  @ResponseStatus(HttpStatus.CREATED)
  public PaginaResponse duplicar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.duplicar(id, principal));
  }

  @PostMapping("/reordenar")
  public void reordenar(@RequestBody ReordenarRequest request, Principal principal) {
    paginaService.reordenar(request.paginaIds(), principal);
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{id}/anexos")
  public List<PaginaAnexoResponse> anexos(@PathVariable UUID id) {
    return paginaAnexoService.listar(id).stream().map(PaginaAnexoResponse::from).toList();
  }

  @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public PaginaAnexoResponse anexar(@PathVariable UUID id, @RequestParam MultipartFile file) {
    return PaginaAnexoResponse.from(paginaAnexoService.anexar(id, file));
  }

  @PreAuthorize(SecurityRoles.READ)
  @GetMapping("/{paginaId}/anexos/{anexoId}/download")
  public ResponseEntity<Resource> baixarAnexo(@PathVariable UUID paginaId, @PathVariable UUID anexoId) {
    var anexo = paginaAnexoService.buscar(anexoId);
    Resource resource = paginaAnexoService.arquivo(paginaId, anexoId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(anexo.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + anexo.getNomeOriginal() + "\"")
        .body(resource);
  }

  @DeleteMapping("/{paginaId}/anexos/{anexoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluirAnexo(@PathVariable UUID paginaId, @PathVariable UUID anexoId) {
    paginaAnexoService.excluir(paginaId, anexoId);
  }
}
