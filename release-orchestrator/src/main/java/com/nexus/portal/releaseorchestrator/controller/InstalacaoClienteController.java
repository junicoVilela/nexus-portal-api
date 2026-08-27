package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarStatusInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.DispararBuildRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ExecutarCicloVidaRequest;
import com.nexus.portal.releaseorchestrator.dto.request.InstalacaoClienteRequest;
import com.nexus.portal.releaseorchestrator.dto.request.RegistrarHealthInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ResolverVersaoInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.response.DeployInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.InstalacaoClienteResponse;
import com.nexus.portal.releaseorchestrator.dto.response.PortasSugeridasResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ResolverVersaoInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.service.DeployInstalacaoService;
import com.nexus.portal.releaseorchestrator.service.InstalacaoClienteService;
import com.nexus.portal.releaseorchestrator.service.ReservaPortaService;
import com.nexus.portal.releaseorchestrator.service.VersaoInstalacaoService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/instalacoes")
public class InstalacaoClienteController {

  private final InstalacaoClienteService service;
  private final ReservaPortaService reservaPortaService;
  private final DeployInstalacaoService deployService;
  private final VersaoInstalacaoService versaoInstalacaoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.INSTALACAO_CRIAR)
  public InstalacaoClienteResponse criar(@Valid @RequestBody InstalacaoClienteRequest request) {
    return InstalacaoClienteResponse.from(service.criar(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public InstalacaoClienteResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody InstalacaoClienteRequest request) {
    return InstalacaoClienteResponse.from(service.atualizar(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public InstalacaoClienteResponse alterarStatus(@PathVariable UUID id,
      @Valid @RequestBody AlterarStatusInstalacaoRequest request) {
    return InstalacaoClienteResponse.from(service.alterarStatus(id, request.status()));
  }

  @PatchMapping("/{id}/health")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public InstalacaoClienteResponse registrarHealth(@PathVariable UUID id,
      @Valid @RequestBody RegistrarHealthInstalacaoRequest request) {
    return InstalacaoClienteResponse.from(service.registrarHealth(id, request));
  }

  @PostMapping("/{id}/start")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public DeployInstalacaoResponse iniciar(
      @PathVariable UUID id,
      @RequestBody(required = false) ExecutarCicloVidaRequest request) {
    return DeployInstalacaoResponse.from(deployService.executarCicloVida(
        id, OperacaoDeploy.INICIAR, request == null ? null : request.modoOuPadrao()));
  }

  @PostMapping("/{id}/stop")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public DeployInstalacaoResponse parar(
      @PathVariable UUID id,
      @RequestBody(required = false) ExecutarCicloVidaRequest request) {
    return DeployInstalacaoResponse.from(deployService.executarCicloVida(
        id, OperacaoDeploy.PARAR, request == null ? null : request.modoOuPadrao()));
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping("/{id}/fontes-versao")
  public FontesVersaoInstalacaoResponse fontesVersao(@PathVariable UUID id) {
    return versaoInstalacaoService.listarFontes(id);
  }

  @PostMapping("/{id}/resolver-versao")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public ResolverVersaoInstalacaoResponse resolverVersao(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ResolverVersaoInstalacaoRequest request) {
    return versaoInstalacaoService.resolver(id, request);
  }

  @PostMapping("/{id}/disparar-build")
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public DispararBuildResponse dispararBuild(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) DispararBuildRequest request) {
    return versaoInstalacaoService.dispararBuild(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.INSTALACAO_EXCLUIR)
  public void excluir(@PathVariable UUID id) {
    service.excluir(id);
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping("/portas-sugeridas")
  public PortasSugeridasResponse sugerirPortas(
      @RequestParam(required = false) UUID hostId,
      @RequestParam(required = false) UUID instalacaoId,
      @RequestParam(required = false) Integer backendInicio,
      @RequestParam(required = false) Integer frontendInicio,
      @RequestParam(required = false) Integer quantidade) {
    int be = backendInicio == null ? ReservaPortaService.BACKEND_INICIO : backendInicio;
    int fe = frontendInicio == null ? ReservaPortaService.FRONTEND_INICIO : frontendInicio;
    int qtd = quantidade == null ? ReservaPortaService.QUANTIDADE : quantidade;
    return reservaPortaService.sugerir(hostId, instalacaoId, be, fe, qtd);
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping("/{id}")
  public InstalacaoClienteResponse buscar(@PathVariable UUID id) {
    return InstalacaoClienteResponse.from(service.buscar(id));
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping
  public PageResponse<InstalacaoClienteResponse> listar(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID clienteId,
      @RequestParam(required = false) UUID hostId,
      @RequestParam(required = false) UUID produtoId,
      @RequestParam(required = false) TipoImplantacao tipoImplantacao,
      @RequestParam(required = false) StatusInstalacao status,
      @RequestParam(required = false) AmbientePadrao ambiente,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("codigo", "nome", "tipoImplantacao", "status", "ambiente", "createdAt", "updatedAt"),
        Sort.by("cliente.sigla").and(Sort.by("host.codigo")).and(Sort.by("ambiente"))
            .and(Sort.by("produto.sigla")));
    return PageResponse.from(
        service.listar(q, clienteId, hostId, produtoId, tipoImplantacao, status, ambiente,
            PageableUtils.of(page, size, sortOrder)),
        InstalacaoClienteResponse::from);
  }
}
