package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.dto.response.DocFlowDashboardResponse;
import br.com.softon.portal.docflow.service.DocFlowDashboardService;
import br.com.softon.portal.shared.security.Permissoes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docflow/dashboard")
@RequiredArgsConstructor
public class DocFlowDashboardController {

  private final DocFlowDashboardService dashboardService;

  @GetMapping("/resumo")
  @PreAuthorize(Permissoes.PAGINA_LER)
  public DocFlowDashboardResponse resumo() {
    return dashboardService.resumo();
  }
}
