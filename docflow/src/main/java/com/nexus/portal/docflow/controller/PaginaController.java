package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.service.PaginaAnexoService;
import com.nexus.portal.docflow.service.PaginaBlocoCatalogoService;
import com.nexus.portal.docflow.service.PaginaEventService;
import com.nexus.portal.docflow.service.PaginaService;
import com.nexus.portal.docflow.service.PaginaTemplateService;
import com.nexus.portal.docflow.service.GeradorPacoteService;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortUtils;
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
import com.nexus.portal.shared.security.Permissoes;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import com.nexus.portal.docflow.dto.request.PaginaRequest;
import com.nexus.portal.docflow.dto.request.ComentarioRevisaoRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateAplicacaoRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateDuplicarRequest;
import com.nexus.portal.docflow.dto.request.ReordenarRequest;
import com.nexus.portal.docflow.dto.response.PaginaAnexoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.docflow.dto.response.PaginaRevisaoResponse;
import com.nexus.portal.docflow.dto.response.PaginaTemplateResponse;
import com.nexus.portal.docflow.dto.response.PaginaTemplateAplicacaoResponse;
import com.nexus.portal.docflow.dto.response.PaginaTemplateVersaoResponse;
import com.nexus.portal.docflow.dto.response.PaginaQualidadeResponse;

@RestController
@RequestMapping("/api/v1/docflow/paginas")
@RequiredArgsConstructor
public class PaginaController {

  private final PaginaService paginaService;
  private final PaginaAnexoService paginaAnexoService;
  private final PaginaBlocoCatalogoService paginaBlocoCatalogoService;
  private final PaginaTemplateService paginaTemplateService;
  private final GeradorPacoteService geradorPacoteService;
  private final PaginaEventService paginaEventService;

  @GetMapping("/blocos")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public List<PaginaBlocoResponse> blocos() {
    return paginaBlocoCatalogoService.listar();
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/templates")
  public List<PaginaTemplateResponse> templates(
      @RequestParam(required = false) UUID projetoId,
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(defaultValue = "false") boolean somenteContexto,
      @RequestParam(defaultValue = "false") boolean incluirArquivados) {
    return paginaTemplateService.listar(projetoId, clienteId, somenteContexto, incluirArquivados).stream()
        .map(template -> PaginaTemplateResponse.from(template,
            paginaTemplateService.paginasOriginadas(template.getId())))
        .toList();
  }

  @PostMapping("/templates")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public PaginaTemplateResponse criarTemplate(@Valid @RequestBody PaginaTemplateRequest request,
      Principal principal) {
    return PaginaTemplateResponse.from(paginaTemplateService.criar(request, principal));
  }

  @PutMapping("/templates/{templateId}")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaTemplateResponse atualizarTemplate(@PathVariable UUID templateId,
      @Valid @RequestBody PaginaTemplateRequest request, Principal principal) {
    return PaginaTemplateResponse.from(paginaTemplateService.atualizar(templateId, request, principal));
  }

  @PostMapping("/templates/{templateId}/duplicar")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public PaginaTemplateResponse duplicarTemplate(@PathVariable UUID templateId,
      @Valid @RequestBody PaginaTemplateDuplicarRequest request, Principal principal) {
    return PaginaTemplateResponse.from(paginaTemplateService.duplicar(templateId, request, principal));
  }

  @PostMapping("/templates/{templateId}/aplicar")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public PaginaTemplateAplicacaoResponse aplicarTemplate(@PathVariable UUID templateId,
      @Valid @RequestBody PaginaTemplateAplicacaoRequest request) {
    return paginaTemplateService.aplicar(templateId, request);
  }

  @PostMapping("/templates/{templateId}/arquivar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaTemplateResponse arquivarTemplate(@PathVariable UUID templateId, Principal principal) {
    return PaginaTemplateResponse.from(paginaTemplateService.definirArquivado(templateId, true, principal));
  }

  @PostMapping("/templates/{templateId}/reativar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaTemplateResponse reativarTemplate(@PathVariable UUID templateId, Principal principal) {
    return PaginaTemplateResponse.from(paginaTemplateService.definirArquivado(templateId, false, principal));
  }

  @GetMapping("/templates/{templateId}/versoes")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public List<PaginaTemplateVersaoResponse> versoesTemplate(@PathVariable UUID templateId) {
    return paginaTemplateService.listarVersoes(templateId).stream()
        .map(versao -> PaginaTemplateVersaoResponse.from(versao,
            paginaTemplateService.paginasOriginadas(templateId, versao.getNumero())))
        .toList();
  }

  @PostMapping("/templates/{templateId}/versoes/{numero}/restaurar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaTemplateResponse restaurarVersaoTemplate(@PathVariable UUID templateId,
      @PathVariable int numero, Principal principal) {
    return PaginaTemplateResponse.from(paginaTemplateService.restaurarVersao(templateId, numero, principal));
  }

  @DeleteMapping("/templates/{templateId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PAGINA_EXCLUIR)
  public void excluirTemplate(@PathVariable UUID templateId, Principal principal) {
    paginaTemplateService.excluir(templateId, principal);
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter eventos() {
    return paginaEventService.inscrever();
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/resumo-por-status")
  public Map<String, Long> resumoPorStatusGlobal() {
    Map<String, Long> map = new LinkedHashMap<>();
    paginaService.resumoContagemPorStatusGlobal().forEach((status, total) -> map.put(status.name(), total));
    return map;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public PaginaResponse criar(@Valid @RequestBody PaginaRequest request, Principal principal) {
    return PaginaResponse.from(paginaService.criar(request, principal));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PaginaRequest request,
      Principal principal) {
    return PaginaResponse.from(paginaService.atualizar(id, request, principal));
  }

  @PutMapping("/{id}/autosave")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse autosave(@PathVariable UUID id, @Valid @RequestBody PaginaRequest request) {
    return PaginaResponse.from(paginaService.autosave(id, request));
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
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

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/{id}")
  public PaginaResponse buscar(@PathVariable UUID id) {
    return PaginaResponse.from(paginaService.buscar(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PAGINA_EXCLUIR)
  public void excluir(@PathVariable UUID id, Principal principal) {
    paginaService.excluir(id, principal);
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/{id}/qualidade")
  public PaginaQualidadeResponse qualidade(@PathVariable UUID id) {
    return PaginaQualidadeResponse.from(paginaService.qualidade(id));
  }

  @PostMapping("/{id}/salvar-rascunho")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse salvarRascunho(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.salvarRascunho(id, principal));
  }

  @PostMapping("/{id}/enviar-revisao")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse enviarRevisao(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.enviarRevisao(id, principal));
  }

  @PostMapping("/{id}/aprovar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse aprovar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.aprovar(id, principal));
  }

  @PostMapping("/{id}/publicar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse publicar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.publicar(id, principal));
  }

  @PostMapping("/{id}/arquivar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaResponse arquivar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.arquivar(id, principal));
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
  public String preview(@PathVariable UUID id) {
    return geradorPacoteService.previewPagina(paginaService.buscar(id));
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/{id}/revisoes")
  public PageResponse<PaginaRevisaoResponse> revisoes(
      @PathVariable UUID id,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("numero", "createdAt", "createdBy", "status", "tipo"),
        Sort.by(Sort.Order.desc("numero")));
    return PageResponse.from(
        paginaService.revisoes(id, PageableUtils.of(page, size, sortOrder)),
        PaginaRevisaoResponse::from);
  }

  @PostMapping("/{id}/revisoes/comentarios")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaRevisaoResponse comentarRevisao(
      @PathVariable UUID id,
      @Valid @RequestBody ComentarioRevisaoRequest request,
      Principal principal) {
    return PaginaRevisaoResponse.from(paginaService.comentarRevisao(id, request.comentario(), principal));
  }

  @PostMapping("/{id}/duplicar")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public PaginaResponse duplicar(@PathVariable UUID id, Principal principal) {
    return PaginaResponse.from(paginaService.duplicar(id, principal));
  }

  @PostMapping("/reordenar")
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public void reordenar(@RequestBody ReordenarRequest request, Principal principal) {
    paginaService.reordenar(request.paginaIds(), principal);
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/anexos")
  public PageResponse<PaginaAnexoResponse> bibliotecaAnexos(
      @RequestParam(required = false) String busca,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "24") Integer size) {
    return PageResponse.from(
        paginaAnexoService.listarBiblioteca(
            busca,
            PageableUtils.of(page, size, Sort.by(Sort.Order.desc("createdAt")))),
        PaginaAnexoResponse::from);
  }

  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping("/{id}/anexos")
  public List<PaginaAnexoResponse> anexos(@PathVariable UUID id) {
    return paginaAnexoService.listar(id).stream().map(PaginaAnexoResponse::from).toList();
  }

  @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public PaginaAnexoResponse anexar(@PathVariable UUID id, @RequestParam MultipartFile file) {
    return PaginaAnexoResponse.from(paginaAnexoService.anexar(id, file));
  }

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
  @PreAuthorize(Permissoes.PAGINA_EDITAR)
  public void excluirAnexo(@PathVariable UUID paginaId, @PathVariable UUID anexoId) {
    paginaAnexoService.excluir(paginaId, anexoId);
  }
}
