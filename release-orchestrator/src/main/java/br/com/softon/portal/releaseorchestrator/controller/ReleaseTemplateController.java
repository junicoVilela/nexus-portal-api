package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusTemplateRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ReleaseTemplateRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ReleaseTemplateResponse;
import br.com.softon.portal.releaseorchestrator.service.ReleaseTemplateService;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.SecurityRoles;
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
@RequestMapping("/api/v1/release-orchestrator/templates")
@PreAuthorize(SecurityRoles.WRITE)
public class ReleaseTemplateController {

    private final ReleaseTemplateService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseTemplateResponse criar(@Valid @RequestBody ReleaseTemplateRequest request) {
        return ReleaseTemplateResponse.from(service.criar(request));
    }

    @PutMapping("/{id}")
    public ReleaseTemplateResponse atualizar(@PathVariable UUID id,
                                             @Valid @RequestBody ReleaseTemplateRequest request) {
        return ReleaseTemplateResponse.from(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ReleaseTemplateResponse alterarStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody AlterarStatusTemplateRequest request) {
        return ReleaseTemplateResponse.from(service.alterarStatus(id, request));
    }

    @PreAuthorize(SecurityRoles.READ)
    @GetMapping
    public PageResponse<ReleaseTemplateResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) SortDirection direction,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size) {
        Sort sortOrder = SortUtils.of(sort, direction,
                List.of("nome", "ativo", "createdAt"), Sort.by("nome"));
        return PageResponse.from(
                service.listar(nome, ativo, PageableUtils.of(page, size, sortOrder)),
                ReleaseTemplateResponse::from);
    }

    @PreAuthorize(SecurityRoles.READ)
    @GetMapping("/{id}")
    public ReleaseTemplateResponse buscar(@PathVariable UUID id) {
        return ReleaseTemplateResponse.from(service.buscar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
