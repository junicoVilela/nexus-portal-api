package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarStatusReleaseRequest;
import com.nexus.portal.releaseorchestrator.dto.request.CancelarReleaseRequest;
import com.nexus.portal.releaseorchestrator.dto.request.DispararBuildRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ManifestoImplantacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.OrigemBuild;
import com.nexus.portal.releaseorchestrator.dto.request.ReleaseRequest;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ReleaseDisponivelDeployResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ReleaseHistoricoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ReleaseResponse;
import com.nexus.portal.releaseorchestrator.dto.response.RevisaoValidacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.service.ManifestoImplantacaoService;
import com.nexus.portal.releaseorchestrator.service.ReleaseDisponivelDeployService;
import com.nexus.portal.releaseorchestrator.service.JenkinsBuildService;
import com.nexus.portal.releaseorchestrator.service.ReleaseService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.nexus.portal.shared.security.Permissoes;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/releases")
public class ReleaseController {

    private final ReleaseService service;
    private final ManifestoImplantacaoService manifestoService;
    private final ReleaseDisponivelDeployService disponivelDeployService;
    private final JenkinsBuildService jenkinsBuildService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permissoes.RELEASE_CRIAR)
    public ReleaseResponse criar(@Valid @RequestBody ReleaseRequest request) {
        Release release = service.criar(request);
        return ReleaseResponse.from(release, service.contarItens(release.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseResponse atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody ReleaseRequest request) {
        Release release = service.atualizar(id, request);
        return ReleaseResponse.from(release, service.contarItens(release.getId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseResponse alterarStatus(@PathVariable UUID id,
                                         @Valid @RequestBody AlterarStatusReleaseRequest request) {
        Release release = service.alterarStatus(id, request);
        return ReleaseResponse.from(release, service.contarItens(release.getId()));
    }

    @PostMapping("/{id}/publicar")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseResponse publicar(@PathVariable UUID id) {
        Release release = service.publicar(id);
        return ReleaseResponse.from(release, service.contarItens(release.getId()));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseResponse cancelar(@PathVariable UUID id,
                                    @RequestBody(required = false) CancelarReleaseRequest request) {
        String motivo = request != null ? request.motivo() : null;
        Release release = service.cancelar(id, motivo);
        return ReleaseResponse.from(release, service.contarItens(release.getId()));
    }

    @PostMapping("/{id}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permissoes.RELEASE_CRIAR)
    public ReleaseResponse duplicar(@PathVariable UUID id) {
        Release release = service.duplicar(id);
        return ReleaseResponse.from(release, 0L);
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping("/{id}/manifestos")
    public List<ManifestoImplantacaoResponse> listarManifestos(@PathVariable UUID id) {
        return manifestoService.listarDaRelease(id);
    }

    @PutMapping("/{id}/manifestos")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ManifestoImplantacaoResponse salvarManifesto(@PathVariable UUID id,
            @Valid @RequestBody ManifestoImplantacaoRequest request) {
        return manifestoService.salvar(id, request);
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping("/{id}/fontes-build")
    public FontesBuildResponse fontesBuild(@PathVariable UUID id) {
        return jenkinsBuildService.listarFontes(id);
    }

    @PostMapping("/{id}/disparar-build")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public DispararBuildResponse dispararBuild(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) DispararBuildRequest request) {
        var body = request != null
            ? request
            : new DispararBuildRequest(OrigemBuild.RELEASE_ATUAL, null);
        return jenkinsBuildService.disparar(id, body);
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping("/{id}/historico")
    public List<ReleaseHistoricoResponse> historico(@PathVariable UUID id) {
        return service.buscarHistorico(id).stream()
                .map(ReleaseHistoricoResponse::from)
                .toList();
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping("/{id}/validar")
    public RevisaoValidacaoResponse validar(@PathVariable UUID id) {
        return service.validar(id);
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping("/disponiveis-deploy")
    public List<ReleaseDisponivelDeployResponse> disponiveisDeploy(@RequestParam UUID produtoId) {
        return disponivelDeployService.listar(produtoId);
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping
    public PageResponse<ReleaseResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) ReleaseStatus status,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) UUID responsavelId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) SortDirection direction,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size) {
        Sort sortOrder = SortUtils.of(sort, direction,
                List.of("versao", "titulo", "status", "tipo", "updatedAt", "createdAt"),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Release> releases = service.listar(q, produtoId, status, tipo, responsavelId,
                sort, PageableUtils.of(page, size, sortOrder));
        return PageResponse.from(releases,
                r -> ReleaseResponse.from(r, service.contarItens(r.getId())));
    }

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping("/{id}")
    public ReleaseResponse buscar(@PathVariable UUID id) {
        Release release = service.buscar(id);
        return ReleaseResponse.from(release, service.contarItens(release.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permissoes.RELEASE_EXCLUIR)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
