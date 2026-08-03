package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarEntregaRascunhoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.CriarEntregaRequest;
import com.nexus.portal.releaseorchestrator.dto.response.EntregaResponse;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.service.EntregaService;
import com.nexus.portal.releaseorchestrator.service.PublicacaoRemotaService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("orchestratorEntregaController")
@RequestMapping("/api/v1/release-orchestrator/entregas")
@Transactional
public class EntregaController {

  private final EntregaService service;
  private final PublicacaoRemotaService publicacaoRemotaService;

  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping
  public PageResponse<EntregaResponse> listar(
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(required = false) UUID produtoId,
      @RequestParam(required = false) StatusEntrega status,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("createdAt", "dataConclusao", "status"),
        Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        service.listar(clienteId, produtoId, status,
            PageableUtils.of(page, size, sortOrder)),
        EntregaResponse::from);
  }

  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping("/{id}")
  public EntregaResponse buscar(@PathVariable UUID id) {
    return EntregaResponse.from(service.buscar(id));
  }

  /**
   * Stream do pacote ZIP gerado para a entrega. Disponível apenas após
   * CONCLUIDA — antes disso o caminho não existe ou está sendo escrito.
   */
  @PreAuthorize(Permissoes.ENTREGA_LER)
  @GetMapping("/{id}/pacote/download")
  public ResponseEntity<Resource> downloadPacote(@PathVariable UUID id) {
    var entrega = service.buscar(id);
    if (entrega.getStatus() != StatusEntrega.CONCLUIDA
        || entrega.getArquivoPacoteCaminho() == null) {
      return ResponseEntity.notFound().build();
    }
    Path arquivo = Path.of(entrega.getArquivoPacoteCaminho());
    if (!Files.isRegularFile(arquivo)) {
      return ResponseEntity.notFound().build();
    }
    String nome = "pacote-" + entrega.getCliente().getSigla().toLowerCase()
        + "-" + entrega.getProduto().getSigla().toLowerCase()
        + "-" + entrega.getRelease().getVersao() + ".zip";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + nome + "\"")
        .contentType(MediaType.parseMediaType("application/zip"))
        .body(new FileSystemResource(arquivo));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.ENTREGA_CRIAR)
  public EntregaResponse criar(@Valid @RequestBody CriarEntregaRequest request) {
    return EntregaResponse.from(service.criar(request));
  }

  @PutMapping("/{id}/rascunho")
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public EntregaResponse atualizarRascunho(@PathVariable UUID id,
      @Valid @RequestBody AtualizarEntregaRascunhoRequest request) {
    return EntregaResponse.from(service.atualizarRascunho(id, request));
  }

  @PostMapping("/{id}/cancelar")
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public EntregaResponse cancelar(@PathVariable UUID id) {
    return EntregaResponse.from(service.cancelar(id));
  }

  /**
   * Cria uma nova entrega RASCUNHO clonando seleção + delta da entrega original.
   * Útil quando uma entrega CONCLUIDA precisa ser refeita (por bug detectado
   * em produção, por exemplo) ou quando uma FALHA precisa de retry com ajustes.
   */
  @PostMapping("/{id}/reentregar")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.ENTREGA_CRIAR)
  public EntregaResponse reentregar(@PathVariable UUID id) {
    return EntregaResponse.from(service.reentregar(id));
  }

  /**
   * Reagenda a publicação remota de uma entrega — útil quando o destino
   * estava fora, o operador corrigiu config e quer disparar nova tentativa
   * sem esperar o backoff. Zera o contador de tentativas e marca PENDENTE.
   */
  @PostMapping("/{id}/publicacao/reagendar")
  @PreAuthorize(Permissoes.ENTREGA_EDITAR)
  public EntregaResponse reagendarPublicacao(@PathVariable UUID id) {
    return EntregaResponse.from(publicacaoRemotaService.reagendar(id));
  }
}
