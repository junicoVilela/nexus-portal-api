package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.dto.response.SessaoResponse;
import com.nexus.identityaccess.service.SessaoService;
import com.nexus.identityaccess.service.SessaoService.SessaoFilter;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/sessoes")
public class SessaoController {

  private final SessaoService service;

  @GetMapping
  @PreAuthorize(Permissoes.AUDITORIA_VISUALIZAR)
  public PageResponse<SessaoResponse> listar(
      @RequestParam(required = false) UUID usuarioId,
      @RequestParam(required = false) Boolean ativa,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("iniciadaEm", "encerradaEm", "ativa"),
        Sort.by(Sort.Order.desc("iniciadaEm")));
    return PageResponse.from(
        service.listar(new SessaoFilter(usuarioId, ativa),
            PageableUtils.of(page, size, sortOrder)),
        SessaoResponse::from);
  }

  @PostMapping("/{id}/revogar")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize(Permissoes.USUARIO_BLOQUEAR)
  public SessaoResponse revogar(@PathVariable UUID id,
      @RequestParam(required = false) String motivo, Principal principal) {
    return SessaoResponse.from(service.revogar(id, motivo, principal));
  }
}
