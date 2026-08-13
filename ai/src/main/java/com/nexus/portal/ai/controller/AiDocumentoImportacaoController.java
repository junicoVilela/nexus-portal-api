package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.request.AiConfirmarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiGerarLoteDocumentoRequest;
import com.nexus.portal.ai.dto.response.AiEstimativaLoteDocumentoResponse;
import com.nexus.portal.ai.dto.response.AiImportacaoDocumentoResponse;
import com.nexus.portal.ai.service.AiDocumentoImportacaoService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai/importacoes")
@RequiredArgsConstructor
public class AiDocumentoImportacaoController {

  private final AiDocumentoImportacaoService service;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiImportacaoDocumentoResponse importarDocumento(
      @RequestParam MultipartFile arquivo,
      @RequestParam(required = false) UUID projetoId,
      @RequestParam(required = false) UUID clienteId,
      Principal principal) {
    return service.importar(arquivo, projetoId, clienteId, principal);
  }

  @GetMapping("/{id}")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public AiImportacaoDocumentoResponse buscarImportacaoDocumento(@PathVariable UUID id, Principal principal) {
    return service.buscar(id, principal);
  }

  @PostMapping("/{id}/estrutura/confirmar")
  @PreAuthorize(
      Permissoes.PAGINA_CRIAR + " and "
          + Permissoes.MODULO_CRIAR + " and "
          + "((#request.modoProjeto().name() == 'NOVO_PROJETO' and "
          + Permissoes.PROJETO_CRIAR
          + ") or (#request.modoProjeto().name() == 'PROJETO_EXISTENTE' and "
          + Permissoes.PROJETO_LER
          + ")) and (#request.modoCliente().name() == 'SEM_CLIENTE' or "
          + Permissoes.CLIENTE_EDITAR
          + ") and (#request.modoCliente().name() != 'NOVO_CLIENTE' or "
          + Permissoes.CLIENTE_CRIAR
          + ")")
  public AiImportacaoDocumentoResponse confirmarEstruturaDocumento(
      @PathVariable UUID id,
      @Valid @RequestBody AiConfirmarEstruturaDocumentoRequest request,
      Principal principal) {
    return service.confirmarEstrutura(id, request, principal);
  }

  @PostMapping("/{id}/paginas/{paginaPlanoId}/selecionar")
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiImportacaoDocumentoResponse selecionarPaginaImportada(
      @PathVariable UUID id,
      @PathVariable UUID paginaPlanoId,
      Principal principal) {
    return service.selecionarPagina(id, paginaPlanoId, principal);
  }

  @PostMapping("/{id}/paginas/{paginaPlanoId}/vincular/{paginaId}")
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiImportacaoDocumentoResponse vincularPaginaImportada(
      @PathVariable UUID id,
      @PathVariable UUID paginaPlanoId,
      @PathVariable UUID paginaId,
      Principal principal) {
    return service.vincularPagina(id, paginaPlanoId, paginaId, principal);
  }

  @PostMapping("/{id}/sincronizar")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public AiImportacaoDocumentoResponse sincronizarImportacaoDocumento(
      @PathVariable UUID id, Principal principal) {
    return service.sincronizar(id, principal);
  }

  @PostMapping("/{id}/lote/estimar")
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiEstimativaLoteDocumentoResponse estimarLoteDocumento(
      @PathVariable UUID id,
      @Valid @RequestBody AiGerarLoteDocumentoRequest request,
      Principal principal) {
    return service.estimarLote(id, request, principal);
  }

  @PostMapping("/{id}/lote/gerar")
  @PreAuthorize(Permissoes.PAGINA_CRIAR)
  public AiImportacaoDocumentoResponse gerarLoteDocumento(
      @PathVariable UUID id,
      @Valid @RequestBody AiGerarLoteDocumentoRequest request,
      Principal principal) {
    return service.gerarLote(id, request, principal);
  }
}
