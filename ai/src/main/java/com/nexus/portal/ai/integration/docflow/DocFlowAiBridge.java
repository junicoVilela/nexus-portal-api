package com.nexus.portal.ai.integration.docflow;

import com.nexus.portal.docflow.dto.request.PaginaRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateAplicacaoRequest;
import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaTemplateAplicacaoResponse;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.repository.PaginaTemplateRepository;
import com.nexus.portal.docflow.service.PaginaBlocoCatalogoService;
import com.nexus.portal.docflow.service.PaginaBlueprintCatalogoService;
import com.nexus.portal.docflow.service.PaginaQualidadeService;
import com.nexus.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import com.nexus.portal.docflow.service.PaginaService;
import com.nexus.portal.docflow.service.PaginaTemplateService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
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
  private final PaginaBlocoCatalogoService paginaBlocoCatalogoService;
  private final PaginaBlueprintCatalogoService paginaBlueprintCatalogoService;

  public DocFlowAiBridge(
      PaginaTemplateService paginaTemplateService,
      PaginaTemplateRepository paginaTemplateRepository,
      PaginaQualidadeService paginaQualidadeService,
      PaginaService paginaService,
      PaginaBlocoCatalogoService paginaBlocoCatalogoService,
      PaginaBlueprintCatalogoService paginaBlueprintCatalogoService) {
    this.paginaTemplateService = paginaTemplateService;
    this.paginaTemplateRepository = paginaTemplateRepository;
    this.paginaQualidadeService = paginaQualidadeService;
    this.paginaService = paginaService;
    this.paginaBlocoCatalogoService = paginaBlocoCatalogoService;
    this.paginaBlueprintCatalogoService = paginaBlueprintCatalogoService;
  }

  public boolean disponivel() {
    return true;
  }

  /**
   * Resolve o modelo da biblioteca: {@code templateId} explícito, senão sugestão por briefing,
   * senão {@link #TEMPLATE_PADRAO}.
   */
  public Optional<PaginaTemplate> buscarTemplate(
      UUID templateId, UUID projetoId, UUID clienteId, String briefing) {
    if (templateId != null) {
      return paginaTemplateRepository.findById(templateId);
    }
    var biblioteca = paginaTemplateService.listar(projetoId, clienteId, false, false);
    return AiTemplateSelector.selecionar(briefing, biblioteca);
  }

  public List<AiTemplateSelector.Recomendacao> recomendarTemplates(
      UUID projetoId, UUID clienteId, String briefing) {
    var biblioteca = paginaTemplateService.listar(projetoId, clienteId, false, false);
    return AiTemplateSelector.recomendar(briefing, biblioteca);
  }

  public List<PaginaBlocoResponse> listarBlocos() {
    return paginaBlocoCatalogoService.listar();
  }

  public Optional<PaginaBlueprintResponse> buscarBlueprint(String templateCodigo) {
    return paginaBlueprintCatalogoService.buscarPorTemplate(templateCodigo);
  }

  public String renderizarBloco(String blocoId, Map<String, String> textos) {
    return paginaBlocoCatalogoService.renderizar(blocoId, textos);
  }

  /** @deprecated use {@link #buscarTemplate(UUID, UUID, UUID, String)} */
  @Deprecated
  public Optional<PaginaTemplate> buscarTemplate(UUID templateId, UUID projetoId, UUID clienteId) {
    return buscarTemplate(templateId, projetoId, clienteId, null);
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
