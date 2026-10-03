package com.nexus.portal.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.request.AiAjustePaginaRequest;
import com.nexus.portal.ai.dto.response.AiAjustePaginaResponse;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge.PaginaAjuste;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.ConflictException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pedido de ajuste de página existente (Fase B, docs/doc-flow/13). Sem triagem: a instrução já é o
 * escopo. Cria a sessão sobre a versão atual da página e enfileira a geração do patch; refinar,
 * rejeitar e aplicar seguem pelos endpoints de sessão.
 */
@Service
public class AiAjustePaginaService {

  private final AiSessaoRepository sessaoRepository;
  private final AiMensagemRepository mensagemRepository;
  private final AiPropostaService propostaService;
  private final DocFlowAiBridge docFlowAiBridge;
  private final AiProperties properties;
  private final ObjectMapper objectMapper;
  private final AuditoriaService auditoriaService;

  public AiAjustePaginaService(
      AiSessaoRepository sessaoRepository,
      AiMensagemRepository mensagemRepository,
      AiPropostaService propostaService,
      DocFlowAiBridge docFlowAiBridge,
      AiProperties properties,
      ObjectMapper objectMapper,
      AuditoriaService auditoriaService) {
    this.sessaoRepository = sessaoRepository;
    this.mensagemRepository = mensagemRepository;
    this.propostaService = propostaService;
    this.docFlowAiBridge = docFlowAiBridge;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.auditoriaService = auditoriaService;
  }

  @Transactional
  public AiAjustePaginaResponse pedir(UUID paginaId, AiAjustePaginaRequest request, Principal principal) {
    if (!properties.enabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Módulo AI desabilitado. Defina nexus.ai.enabled=true.");
    }
    PaginaAjuste pagina = docFlowAiBridge.buscarPaginaParaAjuste(paginaId);
    exigirEditavel(pagina);
    if (pagina.version() != request.version()) {
      throw new ConflictException(
          "A página foi alterada desde que você a abriu. Recarregue o editor antes de pedir o ajuste.");
    }
    AiPaginaEsboco esboco = AiPaginaEsboco.de(pagina.conteudoHtml());
    if (request.secaoId() != null && esboco.secao(request.secaoId()).isEmpty()) {
      throw new BusinessException("Seção " + request.secaoId() + " não encontrada na página.");
    }
    if (request.secaoId() == null && AiPagePatchService.exigeSecao(esboco)) {
      throw new BusinessException("A página é grande demais para ajustar de uma vez. Escolha uma seção.");
    }

    String instrucao = request.instrucao().trim();
    AiSessao sessao = sessaoRepository.save(AiSessao.ajuste(
        instrucao, pagina.projetoId(), pagina.moduloId(), pagina.id(), pagina.version(), request.secaoId()));
    mensagemRepository.save(new AiMensagem(
        sessao, AiPapelMensagem.USUARIO, instrucao, AiPayloadJson.instrucao(objectMapper, instrucao), 1));
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_SESSAO,
        sessao.getId(),
        AiAuditoriaAcoes.SESSAO_CRIADA,
        "Ajuste da página " + pagina.codigoTela()
            + (request.secaoId() == null ? "" : " · seção " + request.secaoId()),
        principal);
    return new AiAjustePaginaResponse(sessao.getId(), propostaService.gerar(sessao.getId(), principal));
  }

  /** Conteúdo aprovado/publicado só muda depois de voltar para rascunho (mesma regra do editor). */
  private static void exigirEditavel(PaginaAjuste pagina) {
    switch (pagina.status()) {
      case "APROVADO", "PUBLICADO" -> throw new BusinessException(
          "Página " + ("PUBLICADO".equals(pagina.status()) ? "publicada" : "aprovada")
              + ": volte para rascunho antes de ajustar o conteúdo.");
      case "ARQUIVADO" -> throw new BusinessException("Páginas arquivadas não podem ser ajustadas.");
      default -> { }
    }
  }
}
