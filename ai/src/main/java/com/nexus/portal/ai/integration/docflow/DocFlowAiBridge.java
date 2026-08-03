package com.nexus.portal.ai.integration.docflow;

import com.nexus.portal.docflow.dto.request.PaginaRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateAplicacaoRequest;
import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.docflow.dto.response.PaginaTemplateAplicacaoResponse;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.repository.PaginaTemplateRepository;
import com.nexus.portal.docflow.service.PaginaQualidadeService;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import com.nexus.portal.docflow.service.PaginaService;
import com.nexus.portal.docflow.service.PaginaTemplateService;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Única ponte DocFlow ↔ AI. Na extração, virar cliente HTTP.
 */
@Component
public class DocFlowAiBridge {

  public static final String TEMPLATE_PADRAO = "FUNCIONALIDADE";

  private final PaginaTemplateService paginaTemplateService;
  private final PaginaTemplateRepository paginaTemplateRepository;
  private final PaginaQualidadeService paginaQualidadeService;
  private final PaginaService paginaService;

  public DocFlowAiBridge(
      PaginaTemplateService paginaTemplateService,
      PaginaTemplateRepository paginaTemplateRepository,
      PaginaQualidadeService paginaQualidadeService,
      PaginaService paginaService) {
    this.paginaTemplateService = paginaTemplateService;
    this.paginaTemplateRepository = paginaTemplateRepository;
    this.paginaQualidadeService = paginaQualidadeService;
    this.paginaService = paginaService;
  }

  public boolean disponivel() {
    return true;
  }

  public Optional<PaginaTemplate> buscarTemplate(UUID templateId, UUID projetoId, UUID clienteId) {
    if (templateId != null) {
      return paginaTemplateRepository.findById(templateId);
    }
    return paginaTemplateService.listar(projetoId, clienteId, false, false).stream()
        .filter(t -> TEMPLATE_PADRAO.equalsIgnoreCase(t.getCodigo()))
        .findFirst()
        .or(() -> paginaTemplateService.listar(projetoId, clienteId, false, false).stream().findFirst());
  }

  public PaginaTemplateAplicacaoResponse aplicarTemplate(
      UUID templateId,
      UUID projetoId,
      UUID moduloId,
      UUID clienteId,
      String titulo,
      String codigoTela) {
    return paginaTemplateService.aplicar(
        templateId,
        new PaginaTemplateAplicacaoRequest(projetoId, moduloId, clienteId, titulo, codigoTela));
  }

  public ResultadoQualidade avaliarQualidade(
      String titulo, String codigoTela, String resumo, String conteudoHtml) {
    Projeto projeto = new Projeto("Contexto AI", "contexto-ai", null, true);
    Modulo modulo = new Modulo("Contexto AI", "contexto-ai", null, 0, true, projeto);
    Pagina pagina = new Pagina(
        titulo == null ? "Sem título" : titulo,
        "rascunho-ai",
        codigoTela == null ? "AI-TEMP" : codigoTela,
        resumo,
        conteudoHtml,
        0,
        true,
        modulo,
        null);
    return paginaQualidadeService.avaliar(pagina);
  }

  public PaginaResponse criarPagina(PaginaRequest request, Principal principal) {
    return PaginaResponse.from(paginaService.criar(request, principal));
  }
}
