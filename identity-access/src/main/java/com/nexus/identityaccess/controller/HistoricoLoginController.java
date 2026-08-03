package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.dto.response.HistoricoLoginResponse;
import com.nexus.identityaccess.service.HistoricoLoginService;
import com.nexus.identityaccess.service.HistoricoLoginService.HistoricoLoginFilter;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/historico-login")
public class HistoricoLoginController {

  private final HistoricoLoginService service;

  @GetMapping
  @PreAuthorize(Permissoes.AUDITORIA_VISUALIZAR)
  public PageResponse<HistoricoLoginResponse> listar(
      @RequestParam(required = false) UUID usuarioId,
      @RequestParam(required = false) String login,
      @RequestParam(required = false) Boolean sucesso,
      @RequestParam(required = false) OffsetDateTime inicio,
      @RequestParam(required = false) OffsetDateTime fim,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("createdAt", "loginInformado", "sucesso"),
        Sort.by(Sort.Order.desc("createdAt")));
    return PageResponse.from(
        service.listar(new HistoricoLoginFilter(usuarioId, login, sucesso, inicio, fim),
            PageableUtils.of(page, size, sortOrder)),
        HistoricoLoginResponse::from);
  }
}
