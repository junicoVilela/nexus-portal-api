package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.identityaccess.service.AuditoriaService.AuditoriaFilter;
import com.nexus.portal.shared.api.PageResponse;
import com.nexus.portal.shared.api.PageableUtils;
import com.nexus.portal.shared.api.SortDirection;
import com.nexus.portal.shared.api.SortUtils;
import com.nexus.portal.shared.security.Permissoes;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.nexus.identityaccess.dto.response.AuditoriaResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/auditoria")
public class AuditoriaController {

  private final AuditoriaService auditoriaService;

  @GetMapping
  @PreAuthorize(Permissoes.AUDITORIA_VISUALIZAR)
  public PageResponse<AuditoriaResponse> recentes(
      @RequestParam(required = false) String usuario,
      @RequestParam(required = false) String acao,
      @RequestParam(required = false) String entidade,
      @RequestParam(required = false) UUID entidadeId,
      @RequestParam(required = false) OffsetDateTime inicio,
      @RequestParam(required = false) OffsetDateTime fim,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("createdAt", "createdBy", "entidade", "acao"),
        Sort.by(Sort.Order.desc("createdAt")));
    AuditoriaFilter filter = new AuditoriaFilter(usuario, acao, entidade, entidadeId, inicio, fim);
    return PageResponse.from(
        auditoriaService.listar(filter, PageableUtils.of(page, size, sortOrder)),
        AuditoriaResponse::from);
  }
}
