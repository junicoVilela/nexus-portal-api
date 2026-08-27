package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.ExecutarDeployLoteRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ExecutarDeployRequest;
import com.nexus.portal.releaseorchestrator.dto.response.AlvosEntregaDeployResponse;
import com.nexus.portal.releaseorchestrator.dto.response.DeployInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.DeployLoteResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import com.nexus.portal.releaseorchestrator.entity.StatusDeploy;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.service.DeployInstalacaoService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/deploys")
public class DeployInstalacaoController {

  private final DeployInstalacaoService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public DeployInstalacaoResponse executar(@Valid @RequestBody ExecutarDeployRequest request) {
    return DeployInstalacaoResponse.from(service.executar(request));
  }

  @PostMapping("/lote")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.INSTALACAO_EDITAR)
  public DeployLoteResponse executarLote(@Valid @RequestBody ExecutarDeployLoteRequest request) {
    AlvosEntregaDeployResponse alvos = service.alvosDaEntrega(request.entregaId());
    List<DeployInstalacaoResponse> itens = new ArrayList<>();
    int concluidos = 0;
    int falhas = 0;
    int ignorados = 0;
    for (AlvosEntregaDeployResponse.Alvo alvo : alvos.alvos()) {
      ModoDeploy modo = request.modo();
      if (modo == ModoDeploy.REAL && alvo.tipoImplantacao() != TipoImplantacao.DOCKER_PULL
          && alvo.tipoImplantacao() != TipoImplantacao.LINUX_MANUAL) {
        modo = ModoDeploy.DRY_RUN;
      }
      DeployInstalacaoResponse d = DeployInstalacaoResponse.from(service.executar(
          new ExecutarDeployRequest(alvos.releaseId(), alvo.instalacaoId(), alvos.entregaId(),
              request.forcar(), modo)));
      itens.add(d);
      if (d.status() == StatusDeploy.CONCLUIDO) {
        concluidos++;
      } else if (d.status() == StatusDeploy.FALHA) {
        falhas++;
      } else if (d.status() == StatusDeploy.IGNORADO) {
        ignorados++;
      }
    }
    return new DeployLoteResponse(itens, concluidos, falhas, ignorados);
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping("/preview")
  public ManifestoImplantacaoResponse preview(
      @RequestParam UUID releaseId,
      @RequestParam UUID instalacaoId) {
    return service.preview(releaseId, instalacaoId);
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping("/{id}")
  public DeployInstalacaoResponse buscar(@PathVariable UUID id) {
    return DeployInstalacaoResponse.from(service.buscar(id));
  }

  @PreAuthorize(Permissoes.INSTALACAO_LER)
  @GetMapping
  public PageResponse<DeployInstalacaoResponse> listar(
      @RequestParam(required = false) UUID instalacaoId,
      @RequestParam(required = false) UUID releaseId,
      @RequestParam(required = false) UUID entregaId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection direction,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    Sort sortOrder = SortUtils.of(sort, direction,
        List.of("createdAt", "status", "modo", "versaoDestino"),
        Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        service.listar(instalacaoId, releaseId, entregaId, PageableUtils.of(page, size, sortOrder)),
        DeployInstalacaoResponse::from);
  }
}
