package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.ClienteLogoService;
import br.com.softon.portal.docflow.service.ClienteService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.Permissoes;
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
import org.springframework.web.multipart.MultipartFile;
import br.com.softon.portal.docflow.dto.request.ClienteRequest;
import br.com.softon.portal.docflow.dto.request.CopiarVinculosRequest;
import br.com.softon.portal.docflow.dto.request.VinculosRequest;
import br.com.softon.portal.docflow.dto.response.ClienteResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/docflow/clientes")
public class ClienteController {
  private final ClienteService clienteService;
  private final ClienteLogoService clienteLogoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.CLIENTE_CRIAR)
  public ClienteResponse criar(@Valid @RequestBody ClienteRequest request) {
    return ClienteResponse.from(clienteService.criar(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteRequest request) {
    return ClienteResponse.from(clienteService.atualizar(id, request));
  }

  @PreAuthorize(Permissoes.CLIENTE_LER)
  @GetMapping
  public PageResponse<ClienteResponse> listar(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("nome", "slug", "ativo", "createdAt", "updatedAt"),
        Sort.by("nome"));
    return PageResponse.from(
        clienteService.listar(nome, PageableUtils.of(page, size, sortOrder)),
        ClienteResponse::from);
  }

  @PreAuthorize(Permissoes.CLIENTE_LER)
  @GetMapping("/{id}")
  public ClienteResponse buscar(@PathVariable UUID id) {
    return ClienteResponse.from(clienteService.buscar(id));
  }

  @PutMapping("/{id}/modulos")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public void vincularModulos(@PathVariable UUID id, @RequestBody VinculosRequest request) {
    clienteService.vincularModulos(id, request.moduloIds());
  }

  @PutMapping("/{id}/projetos")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public void vincularProjetos(@PathVariable UUID id, @RequestBody VinculosRequest request) {
    clienteService.vincularProjetos(id, request.projetoIds());
  }

  @PutMapping("/{id}/paginas")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public void vincularPaginas(@PathVariable UUID id, @RequestBody VinculosRequest request) {
    clienteService.vincularPaginas(id, request.paginaIds());
  }

  @PreAuthorize(Permissoes.CLIENTE_LER)
  @GetMapping("/{id}/vinculos")
  public Map<String, List<UUID>> vinculos(@PathVariable UUID id) {
    return Map.of(
        "projetoIds", clienteService.listarProjetoIds(id),
        "moduloIds", clienteService.listarModuloIds(id),
        "paginaIds", clienteService.listarPaginaIds(id));
  }

  @PostMapping("/{id}/copiar-vinculos")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public void copiarVinculos(@PathVariable UUID id, @Valid @RequestBody CopiarVinculosRequest request) {
    clienteService.copiarVinculos(request.origemClienteId(), id);
  }

  @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public void uploadLogo(@PathVariable UUID id, @RequestParam MultipartFile file) {
    clienteLogoService.salvarLogo(id, file);
  }

  @GetMapping("/{id}/logo")
  public ResponseEntity<Resource> getLogo(@PathVariable UUID id) {
    return clienteLogoService.servir(id);
  }

  @DeleteMapping("/{id}/logo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.CLIENTE_EDITAR)
  public void deleteLogo(@PathVariable UUID id) {
    clienteLogoService.removerLogo(id);
  }
}
