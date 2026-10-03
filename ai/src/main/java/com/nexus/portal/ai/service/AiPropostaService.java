package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest.ModoAplicacao;
import com.nexus.portal.ai.dto.response.AiAplicacaoResponse;
import com.nexus.portal.ai.dto.response.AiJobResponse;
import com.nexus.portal.ai.dto.response.AiPropostaResponse;
import com.nexus.portal.ai.dto.response.AiQualidadeItemResponse;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobTipo;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.docflow.dto.request.PaginaRequest;
import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AiPropostaService {

  private final AiSessaoRepository sessaoRepository;
  private final AiJobRepository jobRepository;
  private final AiPropostaRepository propostaRepository;
  private final AiJobWorkerService jobWorkerService;
  private final AiJobLifecycleService lifecycleService;
  private final AiEventService aiEventService;
  private final AiProperties properties;
  private final DocFlowAiBridge docFlowAiBridge;
  private final ObjectMapper objectMapper;
  private final AiRateLimitService rateLimitService;
  private final AuditoriaService auditoriaService;
  private final AiMensagemRepository mensagemRepository;

  @Transactional
  public AiJobResponse gerar(UUID sessaoId, Principal principal) {
    return gerar(sessaoId, null, principal);
  }

  /**
   * Enfileira a geração. {@code instrucao} (opcional) é o ajuste pedido pelo autor sobre a versão
   * anterior — fica no histórico da sessão e o worker o envia ao modelo junto com a PageSpec anterior.
   */
  @Transactional
  public AiJobResponse gerar(UUID sessaoId, String instrucao, Principal principal) {
    exigirModuloHabilitado();
    AiSessao sessao = sessaoRepository.findByIdForUpdate(sessaoId)
        .filter(s -> s.pertenceA(AiSessaoService.usuario(principal)))
        .orElseThrow(() -> new NotFoundException("Sessão de IA não encontrada."));
    var jobAtivo = jobRepository.findFirstBySessaoIdAndStatusInOrderByCreatedAtDesc(
        sessaoId, AiJobLifecycleService.statusAtivos());
    if (jobAtivo.isPresent()) {
      AiJob atual = jobAtivo.orElseThrow();
      long limiteSegundos = Math.max(properties.timeoutSeconds() * 2L, 300L);
      if (!atual.expirado(OffsetDateTime.now().minusSeconds(limiteSegundos))) {
        return AiJobResponse.from(atual);
      }
      lifecycleService.expirar(
          atual,
          "Heartbeat ausente por mais de " + limiteSegundos + " segundos.");
    }
    if (sessao.getStatus() != AiSessaoStatus.PRONTA_PARA_GERAR
        && sessao.getStatus() != AiSessaoStatus.PRONTA
        && sessao.getStatus() != AiSessaoStatus.ERRO
        && sessao.getStatus() != AiSessaoStatus.GERANDO) {
      throw new BusinessException(
          "Sessão precisa estar pronta ou disponível para uma nova tentativa. Status atual: "
              + sessao.getStatus());
    }

    rateLimitService.exigirGeracaoPermitida(principal);
    if (instrucao != null && !instrucao.isBlank()) {
      int ordem = mensagemRepository.countBySessaoId(sessaoId) + 1;
      mensagemRepository.save(new AiMensagem(
          sessao,
          AiPapelMensagem.USUARIO,
          instrucao.trim(),
          AiPayloadJson.instrucao(objectMapper, instrucao.trim()),
          ordem));
    }
    int tentativa = Math.toIntExact(jobRepository.countBySessaoId(sessaoId) + 1);
    AiJob job = jobRepository.saveAndFlush(
        new AiJob(sessao, AiJobTipo.GERAR_RASCUNHO, tentativa));
    sessao.gerando();
    aiEventService.publicarJob(job);
    agendarProcessamento(job.getId());
    return AiJobResponse.from(job);
  }

  @Transactional(readOnly = true)
  public AiPropostaResponse propostaAtual(UUID sessaoId, Principal principal) {
    carregarSessao(sessaoId, principal);
    AiProposta proposta = propostaRepository
        .findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(sessaoId, AiPropostaStatus.PENDENTE)
        .or(() -> propostaRepository.findFirstBySessaoIdOrderByCreatedAtDesc(sessaoId))
        .orElseThrow(() -> new NotFoundException("Nenhuma proposta encontrada para a sessão."));
    return toResponse(proposta);
  }

  @Transactional
  public AiAplicacaoResponse aplicar(UUID sessaoId, AplicarAiPropostaRequest request, Principal principal) {
    exigirModuloHabilitado();
    AiSessao sessao = carregarSessao(sessaoId, principal);
    AiProposta proposta = propostaRepository
        .findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(sessaoId, AiPropostaStatus.PENDENTE)
        .orElseThrow(() -> new NotFoundException("Nenhuma proposta pendente para aplicar."));

    ModoAplicacao modo = request.modo();
    if (modo == ModoAplicacao.FORM) {
      auditoriaService.registrar(
          AiAuditoriaAcoes.ENTIDADE_PROPOSTA,
          proposta.getId(),
          AiAuditoriaAcoes.PROPOSTA_APLICADA_FORM,
          truncar(proposta.getTitulo() + " · " + proposta.getCodigoTela(), 200),
          principal);
      return new AiAplicacaoResponse(
          modo.name(),
          proposta.getId(),
          null,
          proposta.getTitulo(),
          proposta.getSlug(),
          proposta.getCodigoTela(),
          proposta.getResumo(),
          proposta.getConteudoHtml(),
          proposta.getTemplateId(),
          proposta.getTemplateVersao(),
          request.moduloId() != null ? request.moduloId() : sessao.getModuloId());
    }

    UUID moduloId = request.moduloId() != null ? request.moduloId() : sessao.getModuloId();
    if (moduloId == null) {
      throw new BusinessException("moduloId é obrigatório para persistir a página.");
    }
    PaginaRequest paginaRequest = new PaginaRequest(
        proposta.getTitulo(),
        proposta.getSlug(),
        proposta.getCodigoTela(),
        proposta.getResumo(),
        proposta.getConteudoHtml(),
        request.ordem() == null ? 0 : request.ordem(),
        true,
        moduloId,
        request.parentId(),
        null,
        proposta.getTemplateId(),
        proposta.getTemplateVersao());
    PaginaResponse criada = docFlowAiBridge.criarPagina(paginaRequest, principal);
    proposta.aceitar(criada.id());
    sessao.aplicada();
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_PROPOSTA,
        proposta.getId(),
        AiAuditoriaAcoes.PROPOSTA_ACEITA,
        truncar("página " + criada.id() + " · " + proposta.getCodigoTela(), 200),
        principal);
    return new AiAplicacaoResponse(
        modo.name(),
        proposta.getId(),
        criada.id(),
        proposta.getTitulo(),
        proposta.getSlug(),
        proposta.getCodigoTela(),
        proposta.getResumo(),
        proposta.getConteudoHtml(),
        proposta.getTemplateId(),
        proposta.getTemplateVersao(),
        moduloId);
  }

  /** Autor descarta a proposta; a sessão continua disponível para regenerar com uma instrução. */
  @Transactional
  public AiPropostaResponse rejeitar(UUID sessaoId, String motivo, Principal principal) {
    exigirModuloHabilitado();
    carregarSessao(sessaoId, principal);
    AiProposta proposta = propostaRepository
        .findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(sessaoId, AiPropostaStatus.PENDENTE)
        .orElseThrow(() -> new NotFoundException("Nenhuma proposta pendente para rejeitar."));
    proposta.rejeitar(motivo);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_PROPOSTA,
        proposta.getId(),
        AiAuditoriaAcoes.PROPOSTA_REJEITADA,
        truncar(proposta.getMotivoRejeicao() == null ? "sem motivo" : proposta.getMotivoRejeicao(), 200),
        principal);
    return toResponse(proposta);
  }

  private void agendarProcessamento(UUID jobId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          jobWorkerService.processar(jobId);
        }
      });
    } else {
      jobWorkerService.processar(jobId);
    }
  }

  private AiPropostaResponse toResponse(AiProposta proposta) {
    boolean apto = false;
    List<AiQualidadeItemResponse> itens = List.of();
    try {
      if (proposta.getQualidadeJson() != null) {
        JsonNode root = objectMapper.readTree(proposta.getQualidadeJson());
        apto = root.path("aptoParaRevisao").asBoolean(false);
        itens = objectMapper.convertValue(
            root.path("itens"),
            new TypeReference<List<AiQualidadeItemResponse>>() {});
      }
    } catch (Exception ignored) {
      itens = List.of();
    }
    return AiPropostaResponse.from(proposta, apto, itens);
  }

  private AiSessao carregarSessao(UUID id, Principal principal) {
    return sessaoRepository.findById(id)
        .filter(sessao -> sessao.pertenceA(AiSessaoService.usuario(principal)))
        .orElseThrow(() -> new NotFoundException("Sessão de IA não encontrada."));
  }

  private void exigirModuloHabilitado() {
    if (!properties.enabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Módulo AI desabilitado. Defina nexus.ai.enabled=true.");
    }
  }

  private static String truncar(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
