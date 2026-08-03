package com.nexus.portal.releaseorchestrator.controller;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarStatusTemplateRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ReleaseTemplateRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ReleaseTemplateResponse;
import com.nexus.portal.releaseorchestrator.service.ReleaseTemplateService;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/release-orchestrator/templates")
public class ReleaseTemplateController {

    private final ReleaseTemplateService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permissoes.TEMPLATE_CRIAR)
    public ReleaseTemplateResponse criar(@Valid @RequestBody ReleaseTemplateRequest request) {
        return ReleaseTemplateResponse.from(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissoes.TEMPLATE_EDITAR)
    public ReleaseTemplateResponse atualizar(@PathVariable UUID id,
                                             @Valid @RequestBody ReleaseTemplateRequest request) {
        return ReleaseTemplateResponse.from(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Permissoes.TEMPLATE_EDITAR)
    public ReleaseTemplateResponse alterarStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody AlterarStatusTemplateRequest request) {
        return ReleaseTemplateResponse.from(service.alterarStatus(id, request));
    }

    @PreAuthorize(Permissoes.TEMPLATE_LER)
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

    @PreAuthorize(Permissoes.TEMPLATE_LER)
    @GetMapping("/{id}")
    public ReleaseTemplateResponse buscar(@PathVariable UUID id) {
        return ReleaseTemplateResponse.from(service.buscar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permissoes.TEMPLATE_EXCLUIR)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
