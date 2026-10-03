package com.nexus.portal.docflow.controller;

import com.nexus.portal.docflow.service.ProjetoRagService;
import com.nexus.portal.docflow.service.ProjetoService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
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
import com.nexus.portal.docflow.dto.request.ProjetoRequest;
import com.nexus.portal.docflow.dto.response.ProjetoResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/docflow/projetos")
public class ProjetoController {
  private final ProjetoService projetoService;
  private final ProjetoRagService projetoRagService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PROJETO_CRIAR)
  public ProjetoResponse criar(@Valid @RequestBody ProjetoRequest request) {
    return ProjetoResponse.from(projetoService.criar(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.PROJETO_EDITAR)
  public ProjetoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ProjetoRequest request) {
    return ProjetoResponse.from(projetoService.atualizar(id, request));
  }

  @PreAuthorize(Permissoes.PROJETO_LER)
  @GetMapping
  public PageResponse<ProjetoResponse> listar(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("nome", "slug", "ativo", "createdAt", "updatedAt"),
        Sort.by("nome"));
    return PageResponse.from(
        projetoService.listar(nome, PageableUtils.of(page, size, sortOrder)),
        ProjetoResponse::from);
  }

  @PreAuthorize(Permissoes.PROJETO_LER)
  @GetMapping("/{id}")
  public ProjetoResponse buscar(@PathVariable UUID id) {
    return ProjetoResponse.from(projetoService.buscar(id));
  }

  /**
   * Base de RAG do projeto: um Markdown por tela publicada + {@code index.json} (sha256 por
   * arquivo), {@code llms.txt} e {@code llms-full.txt}. Formato em {@code ManualRagService}.
   */
  @PreAuthorize(Permissoes.PAGINA_LER)
  @GetMapping(value = "/{id}/rag.zip", produces = "application/zip")
  public ResponseEntity<byte[]> exportarRag(@PathVariable UUID id) {
    ProjetoRagService.Exportacao exportacao = projetoRagService.exportar(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(exportacao.nomeArquivo()).build().toString())
        .contentType(MediaType.parseMediaType("application/zip"))
        .body(exportacao.zip());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.PROJETO_EXCLUIR)
  public void excluir(@PathVariable UUID id, Principal principal) {
    projetoService.excluir(id, principal);
  }
}
