package com.nexus.portal.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.request.AiMensagemRequest;
import com.nexus.portal.ai.dto.request.CriarAiSessaoRequest;
import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.dto.response.AiMensagemResponse;
import com.nexus.portal.ai.dto.response.AiSessaoResponse;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.ai.service.AiTriagemService.ResultadoTriagem;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiSessaoService {

  private final AiSessaoRepository sessaoRepository;
  private final AiMensagemRepository mensagemRepository;
  private final AiTriagemService triagemService;
  private final AiProperties properties;
  private final ObjectMapper objectMapper;
  private final AuditoriaService auditoriaService;
  private final AiTemplateRecomendacaoService templateRecomendacaoService;
  private final AiJobLifecycleService jobLifecycleService;

  public AiSessaoService(
      AiSessaoRepository sessaoRepository,
      AiMensagemRepository mensagemRepository,
      AiTriagemService triagemService,
      AiProperties properties,
      ObjectMapper objectMapper,
      AuditoriaService auditoriaService,
      AiTemplateRecomendacaoService templateRecomendacaoService,
      AiJobLifecycleService jobLifecycleService) {
    this.sessaoRepository = sessaoRepository;
    this.mensagemRepository = mensagemRepository;
    this.triagemService = triagemService;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.auditoriaService = auditoriaService;
    this.templateRecomendacaoService = templateRecomendacaoService;
    this.jobLifecycleService = jobLifecycleService;
  }

  @Transactional
  public AiSessaoResponse criar(CriarAiSessaoRequest request, Principal principal) {
    exigirModuloHabilitado();
    validarObjetivo(request);

    UUID templateId = resolverTemplate(request);
    List<String> componentesSelecionados = templateRecomendacaoService.validarComponentes(
        request.briefing(),
        request.projetoId(),
        request.clienteId(),
        templateId,
        request.componentesSelecionados());
    AiSessao sessao = new AiSessao(
        request.objetivo(),
        request.briefing().trim(),
        request.projetoId(),
        request.moduloId(),
        request.clienteId(),
        request.paginaId(),
        templateId,
        componentesSelecionados);
    sessaoRepository.save(sessao);

    adicionarMensagem(
        sessao,
        AiPapelMensagem.USUARIO,
        request.briefing().trim(),
        AiPayloadJson.respostas(objectMapper, Map.of()));

    ResultadoTriagem triagem = triagemService.avaliar(request.objetivo(), request.briefing(), Map.of());
    aplicarTriagem(sessao, triagem);

    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_SESSAO,
        sessao.getId(),
        AiAuditoriaAcoes.SESSAO_CRIADA,
        "Objetivo " + sessao.getObjetivo() + " · briefing " + sessao.getBriefing().length()
            + " chars · " + componentesSelecionados.size() + " componentes selecionados",
        principal);

    return montarResponse(sessao);
  }

  @Transactional(readOnly = true)
  public AiSessaoResponse buscar(UUID id, Principal principal) {
    AiSessao sessao = carregar(id, principal);
    return montarResponse(sessao);
  }

  @Transactional
  public AiSessaoResponse enviarMensagem(UUID id, AiMensagemRequest request, Principal principal) {
    exigirModuloHabilitado();
    AiSessao sessao = carregar(id, principal);
    garantirEditavel(sessao);

    Map<String, String> respostas = request.respostas() == null ? Map.of() : request.respostas();
    adicionarMensagem(
        sessao,
        AiPapelMensagem.USUARIO,
        request.conteudo().trim(),
        AiPayloadJson.respostas(objectMapper, respostas));

    Map<String, String> acumulado = coletarRespostas(sessao.getId());
    ResultadoTriagem triagem = triagemService.avaliar(sessao.getObjetivo(), sessao.getBriefing(), acumulado);
    aplicarTriagem(sessao, triagem);

    return montarResponse(sessao);
  }

  @Transactional
  public AiSessaoResponse cancelar(UUID id, Principal principal) {
    AiSessao sessao = carregar(id, principal);
    if (sessao.getStatus() == AiSessaoStatus.APLICADA) {
      throw new BusinessException("Sessão já aplicada não pode ser cancelada.");
    }
    if (!sessao.cancelada()) {
      sessao.cancelar();
      jobLifecycleService.cancelarSessao(sessao.getId());
      adicionarMensagem(sessao, AiPapelMensagem.SISTEMA, "Sessão cancelada pelo usuário.", null);
      auditoriaService.registrar(
          AiAuditoriaAcoes.ENTIDADE_SESSAO,
          sessao.getId(),
          AiAuditoriaAcoes.SESSAO_CANCELADA,
          "Sessão cancelada",
          principal);
    }
    return montarResponse(sessao);
  }

  private void aplicarTriagem(AiSessao sessao, ResultadoTriagem triagem) {
    if (triagem.completa()) {
      sessao.prontaParaGerar();
      adicionarMensagem(
          sessao,
          AiPapelMensagem.ASSISTENTE,
          triagem.mensagem(),
          AiPayloadJson.perguntas(objectMapper, List.of(), triagem.contextoExtraido()));
    } else {
      sessao.aguardarUsuario();
      adicionarMensagem(
          sessao,
          AiPapelMensagem.ASSISTENTE,
          triagem.mensagem(),
          AiPayloadJson.perguntas(objectMapper, triagem.perguntas(), triagem.contextoExtraido()));
    }
  }

  private void adicionarMensagem(AiSessao sessao, AiPapelMensagem papel, String conteudo, String payload) {
    int ordem = mensagemRepository.countBySessaoId(sessao.getId()) + 1;
    mensagemRepository.save(new AiMensagem(sessao, papel, conteudo, payload, ordem));
  }

  private Map<String, String> coletarRespostas(UUID sessaoId) {
    Map<String, String> acumulado = new LinkedHashMap<>();
    for (AiMensagem mensagem : mensagemRepository.findBySessaoIdOrderByOrdemAsc(sessaoId)) {
      if (mensagem.getPapel() == AiPapelMensagem.USUARIO) {
        acumulado.putAll(AiPayloadJson.lerRespostas(objectMapper, mensagem.getPayloadJson()));
      }
    }
    return acumulado;
  }

  private AiSessaoResponse montarResponse(AiSessao sessao) {
    List<AiMensagemResponse> mensagens = mensagemRepository
        .findBySessaoIdOrderByOrdemAsc(sessao.getId())
        .stream()
        .map(m -> AiMensagemResponse.from(m, AiPayloadJson.lerPerguntas(objectMapper, m.getPayloadJson())))
        .toList();
    return AiSessaoResponse.from(
        sessao,
        mensagens,
        jobLifecycleService.atual(sessao.getId()).orElse(null));
  }

  private AiSessao carregar(UUID id, Principal principal) {
    return sessaoRepository.findById(id)
        .filter(sessao -> sessao.pertenceA(usuario(principal)))
        .orElseThrow(() -> new NotFoundException("Sessão de IA não encontrada."));
  }

  static String usuario(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }

  private void exigirModuloHabilitado() {
    if (!properties.enabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Módulo AI desabilitado. Defina nexus.ai.enabled=true.");
    }
  }

  private void garantirEditavel(AiSessao sessao) {
    if (sessao.terminal()) {
      throw new BusinessException("Sessão encerrada (" + sessao.getStatus() + ").");
    }
    if (sessao.getStatus() == AiSessaoStatus.GERANDO) {
      throw new BusinessException("Sessão em geração; aguarde o término do job.");
    }
  }

  private void validarObjetivo(CriarAiSessaoRequest request) {
    // Fase B: o worker ainda não lê o HTML atual nem aplica via atualização de página.
    if (request.objetivo() == AiObjetivo.ATUALIZAR_PAGINA) {
      throw new BusinessException(
          "Ajustar uma página existente com IA ainda não está disponível. Crie uma nova página.");
    }
  }

  private UUID resolverTemplate(CriarAiSessaoRequest request) {
    if (request.templateId() != null) {
      return request.templateId();
    }
    var recomendacao = templateRecomendacaoService.recomendar(
        new AiTemplateRecomendacaoRequest(
            request.briefing(), request.projetoId(), request.clienteId(), null));
    return recomendacao.exigeConfirmacao() || recomendacao.recomendado() == null
        ? null
        : recomendacao.recomendado().templateId();
  }
}
