package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ProdutoRhRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ProdutoRhResponse;
import br.com.softon.portal.releaseorchestrator.service.ProdutoRhService;
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
@RequestMapping("/api/v1/release-orchestrator/produtos")
@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
public class ProdutoRhController {

    private final ProdutoRhService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoRhResponse criar(@Valid @RequestBody ProdutoRhRequest request) {
        return ProdutoRhResponse.from(service.criar(request));
    }

    @PutMapping("/{id}")
    public ProdutoRhResponse atualizar(@PathVariable UUID id,
                                       @Valid @RequestBody ProdutoRhRequest request) {
        return ProdutoRhResponse.from(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ProdutoRhResponse alterarStatus(@PathVariable UUID id,
                                           @Valid @RequestBody AlterarStatusProdutoRequest request) {
        return ProdutoRhResponse.from(service.alterarStatus(id, request));
    }

    @GetMapping
    public PageResponse<ProdutoRhResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) SortDirection direction,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Sort sortOrder = SortUtils.of(sort, direction,
                List.of("nome", "sigla", "ativo", "createdAt"), Sort.by("nome"));
        return PageResponse.from(
                service.listar(nome, ativo, PageableUtils.of(page, size, sortOrder)),
                ProdutoRhResponse::from);
    }

    @GetMapping("/{id}")
    public ProdutoRhResponse buscar(@PathVariable UUID id) {
        return ProdutoRhResponse.from(service.buscar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
