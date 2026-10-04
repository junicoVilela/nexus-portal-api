package com.nexus.portal.ai.service;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.dto.request.AiFilaAceitarRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest.ModoAplicacao;
import com.nexus.portal.ai.dto.request.RejeitarAiPropostaRequest;
import com.nexus.portal.ai.dto.response.AiAplicacaoResponse;
import com.nexus.portal.ai.dto.response.AiFilaPrItemResponse;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiPrEventoStatus;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiPrEventoRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fila de propostas vindas de PR (AI-608…610). Quem tem {@code PAGINA:AI_PROPOSTA} vê tudo; ao
 * agir num item ele o <b>assume</b>: a sessão passa a ser dele e os fluxos normais (assistente,
 * editor, ajuste) funcionam sem regra especial.
 */
@Service
public class AiFilaPrService {

  /** A fila mostra os itens mais recentes; PRs mergeados chegam em volume baixo. */
  static final int LIMITE = 200;

  private final AiPrEventoRepository eventoRepository;
  private final AiSessaoRepository sessaoRepository;
  private final AiPropostaService propostaService;
  private final AiPrIngestaoService ingestaoService;
  private final AuditoriaService auditoriaService;
  private final DocFlowAiBridge docFlowAiBridge;

  public AiFilaPrService(
      AiPrEventoRepository eventoRepository,
      AiSessaoRepository sessaoRepository,
      AiPropostaService propostaService,
      AiPrIngestaoService ingestaoService,
      AuditoriaService auditoriaService,
      DocFlowAiBridge docFlowAiBridge) {
    this.eventoRepository = eventoRepository;
    this.sessaoRepository = sessaoRepository;
    this.propostaService = propostaService;
    this.ingestaoService = ingestaoService;
    this.auditoriaService = auditoriaService;
    this.docFlowAiBridge = docFlowAiBridge;
  }

  @Transactional(readOnly = true)
  public List<AiFilaPrItemResponse> listar(boolean somentePendentes) {
    var status = somentePendentes
        ? EnumSet.complementOf(EnumSet.of(AiPrEventoStatus.IGNORADO))
        : EnumSet.allOf(AiPrEventoStatus.class);
    return eventoRepository.findByStatusInOrderByCreatedAtDesc(status, PageRequest.of(0, LIMITE)).stream()
        .map(this::item)
        .filter(item -> !somentePendentes || item.pendente())
        .toList();
  }

  @Transactional(readOnly = true)
  public AiFilaPrItemResponse buscar(UUID id) {
    return item(carregar(id));
  }

  /** O item passa a ser do autor: a sessão da IA também (para regenerar, abrir no editor…). */
  @Transactional
  public AiFilaPrItemResponse assumir(UUID id, Principal principal) {
    AiPrEvento evento = carregar(id);
    assumir(evento, principal);
    return item(evento);
  }

  @Transactional
  public AiFilaPrItemResponse rejeitar(UUID id, RejeitarAiPropostaRequest request, Principal principal) {
    AiPrEvento evento = carregar(id);
    assumir(evento, principal);
    propostaService.rejeitar(evento.getSessaoId(),
        request == null ? null : request.categoria(),
        request == null ? null : request.motivo(),
        principal);
    return item(evento);
  }

  /** Página nova: cria o rascunho direto da fila. Ajustes são revisados no editor. */
  @Transactional
  public AiAplicacaoResponse aceitar(UUID id, AiFilaAceitarRequest request, Principal principal) {
    AiPrEvento evento = carregar(id);
    AiSessao sessao = assumir(evento, principal);
    if (sessao.getObjetivo() != AiObjetivo.CRIAR_PAGINA) {
      throw new BusinessException("Ajustes de página são revisados no editor: use \"Abrir no editor\".");
    }
    return propostaService.aplicar(sessao.getId(), new AplicarAiPropostaRequest(
        ModoAplicacao.PERSISTIR,
        request == null ? null : request.moduloId(),
        request == null ? null : request.parentId(),
        null), principal);
  }

  /** Itens em erro ou aguardando rascunho: tenta de novo (depois de o autor voltar a página a rascunho). */
  public AiFilaPrItemResponse reprocessar(UUID id) {
    AiPrEvento evento = carregar(id);
    if (!evento.podeReprocessar()) {
      throw new BusinessException("Este item já tem proposta; regenere pelo assistente ou pelo editor.");
    }
    ingestaoService.processar(id);
    return item(carregar(id));
  }

  private AiSessao assumir(AiPrEvento evento, Principal principal) {
    if (evento.getSessaoId() == null) {
      throw new BusinessException("Este item ainda não tem proposta.");
    }
    AiSessao sessao = sessaoRepository.findById(evento.getSessaoId())
        .orElseThrow(() -> new NotFoundException("Sessão de IA do item não encontrada."));
    String usuario = AiSessaoService.usuario(principal);
    if (!sessao.pertenceA(usuario)) {
      sessao.transferirPara(usuario);
      evento.assumir(usuario);
      auditoriaService.registrar(AiAuditoriaAcoes.ENTIDADE_PR_EVENTO, evento.getId(), AiAuditoriaAcoes.PR_ASSUMIDO,
          evento.getRepositorio() + (evento.getNumeroPr() == null ? "" : "#" + evento.getNumeroPr()) + " · "
              + evento.getCodigoTela(), principal);
    }
    return sessao;
  }

  private AiFilaPrItemResponse item(AiPrEvento evento) {
    long capturas = docFlowAiBridge.capturasDaTela(evento.getCodigoTela());
    if (evento.getSessaoId() == null) {
      return AiFilaPrItemResponse.from(evento, null, null, capturas);
    }
    var sessaoStatus = sessaoRepository.findById(evento.getSessaoId()).map(AiSessao::getStatus).orElse(null);
    return AiFilaPrItemResponse.from(evento, sessaoStatus,
        propostaService.ultimaPropostaDaFila(evento.getSessaoId()).orElse(null), capturas);
  }

  /** "Revisado, a página já está certa": tira o item da fila sem gerar nada. */
  @Transactional
  public AiFilaPrItemResponse dispensar(UUID id, Principal principal) {
    AiPrEvento evento = carregar(id);
    if (!evento.podeReprocessar()) {
      throw new BusinessException("Este item já tem proposta: rejeite a proposta em vez de dispensar.");
    }
    evento.dispensar(AiSessaoService.usuario(principal));
    return item(evento);
  }

  private AiPrEvento carregar(UUID id) {
    return eventoRepository.findById(id).orElseThrow(() -> new NotFoundException("Item da fila não encontrado."));
  }
}
