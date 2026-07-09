package br.com.softon.rbac.controller;

import br.com.softon.rbac.service.AuditoriaService;
import br.com.softon.rbac.entity.AuditoriaEvento;
import br.com.softon.portal.shared.api.PageResponse;
import br.com.softon.portal.shared.api.PageableUtils;
import br.com.softon.portal.shared.api.SortDirection;
import br.com.softon.portal.shared.api.SortUtils;
import br.com.softon.portal.shared.security.Permissoes;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.softon.rbac.dto.response.AuditoriaResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/auditoria")
public class AuditoriaController {

  private final AuditoriaService auditoriaService;

  @GetMapping
  @PreAuthorize(Permissoes.AUDITORIA_VISUALIZAR)
  public PageResponse<AuditoriaResponse> recentes(
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) SortDirection dir,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size) {
    Sort sortOrder = SortUtils.of(sort, dir,
        List.of("createdAt", "createdBy", "entidade", "acao"),
        Sort.by(Sort.Order.desc("createdAt")));
    return PageResponse.from(
        auditoriaService.listar(PageableUtils.of(page, size, sortOrder)),
        AuditoriaResponse::from);
  }
}
