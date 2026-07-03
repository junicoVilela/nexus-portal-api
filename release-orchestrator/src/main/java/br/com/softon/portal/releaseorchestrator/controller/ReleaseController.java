package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusReleaseRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.CancelarReleaseRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ReleaseRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ReleaseHistoricoResponse;
import br.com.softon.portal.releaseorchestrator.dto.response.ReleaseResponse;
import br.com.softon.portal.releaseorchestrator.dto.response.RevisaoValidacaoResponse;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.service.ReleaseService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.Permissoes;
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
