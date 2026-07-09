package br.com.softon.rbac.controller;

import br.com.softon.rbac.dto.request.AcessoTemporarioRequest;
import br.com.softon.rbac.dto.response.AcessoTemporarioResponse;
import br.com.softon.rbac.service.AcessoTemporarioService;
import br.com.softon.rbac.service.AcessoTemporarioService.AcessoTemporarioFilter;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import br.com.softon.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
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
@RequestMapping("/api/v1/rbac/acessos-temporarios")
public class AcessoTemporarioController {

  private final AcessoTemporarioService service;

  @GetMapping
  @PreAuthorize(Permissoes.USUARIO_LER)
  public PageResponse<AcessoTemporarioResponse> listar(
      @RequestParam(required = false) UUID usuarioId,
      @RequestParam(required = false) UUID grupoId,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("createdAt", "inicioEm", "fimEm"),
        Sort.by(Sort.Order.desc("createdAt")));
    return PageResponse.from(
        service.listar(new AcessoTemporarioFilter(usuarioId, grupoId),
            PageableUtils.of(page, size, sortOrder)),
        AcessoTemporarioResponse::from);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public AcessoTemporarioResponse criar(@Valid @RequestBody AcessoTemporarioRequest req,
      Principal principal) {
    return AcessoTemporarioResponse.from(service.criar(
        req.usuarioId(), req.grupoAcessoId(), req.permissaoId(), req.escopoAcessoId(),
        req.inicioEm(), req.fimEm(), req.justificativa(), principal));
  }

  @PostMapping("/{id}/revogar")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public AcessoTemporarioResponse revogar(@PathVariable UUID id,
      @RequestParam(required = false) String motivo, Principal principal) {
    return AcessoTemporarioResponse.from(service.revogar(id, motivo, principal));
  }
}
