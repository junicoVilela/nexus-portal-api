package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.dto.request.ReleaseItemRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ReordenarItensRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.ReleaseItemResponse;
import br.com.softon.portal.releaseorchestrator.service.ReleaseItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.softon.portal.shared.security.Permissoes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/release-orchestrator/releases/{releaseId}/itens")
public class ReleaseItemController {

    private final ReleaseItemService service;

    @PreAuthorize(Permissoes.RELEASE_LER)
    @GetMapping
    public List<ReleaseItemResponse> listar(@PathVariable UUID releaseId) {
        return service.listar(releaseId).stream()
                .map(ReleaseItemResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseItemResponse adicionar(@PathVariable UUID releaseId,
                                         @Valid @RequestBody ReleaseItemRequest request) {
        return ReleaseItemResponse.from(service.adicionar(releaseId, request));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseItemResponse atualizar(@PathVariable UUID releaseId,
                                          @PathVariable UUID itemId,
                                          @Valid @RequestBody ReleaseItemRequest request) {
        return ReleaseItemResponse.from(service.atualizar(releaseId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public void remover(@PathVariable UUID releaseId, @PathVariable UUID itemId) {
        service.remover(releaseId, itemId);
    }

    @PutMapping("/reordenar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public void reordenar(@PathVariable UUID releaseId,
                          @Valid @RequestBody ReordenarItensRequest request) {
        service.reordenar(releaseId, request);
    }

    @PostMapping("/{itemId}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permissoes.RELEASE_EDITAR)
    public ReleaseItemResponse duplicar(@PathVariable UUID releaseId,
                                         @PathVariable UUID itemId) {
        return ReleaseItemResponse.from(service.duplicar(releaseId, itemId));
    }
}
