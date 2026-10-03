package com.nexus.portal.ai.service;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.dto.response.AiJobResponse;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobEtapa;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiJobLifecycleService {

  private static final List<AiJobStatus> STATUS_ATIVOS = List.copyOf(
      EnumSet.of(AiJobStatus.PENDENTE, AiJobStatus.PROCESSANDO));

  private final AiJobRepository jobRepository;
  private final AiPropostaRepository propostaRepository;
  private final AiEventService aiEventService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public Optional<ContextoExecucao> iniciar(UUID jobId, String modelo) {
    AiJob job = carregarParaAtualizar(jobId);
    AiSessao sessao = job.getSessao();
    if (sessao.cancelada() || job.getStatus() == AiJobStatus.CANCELADO) {
      cancelar(job);
      return Optional.empty();
    }
    if (job.getStatus() != AiJobStatus.PENDENTE) {
      return Optional.empty();
    }

    job.iniciar(modelo);
    job.atualizarProgresso(AiJobEtapa.PREPARANDO_CONTEXTO, 10);
    aiEventService.publicarJob(job);
    String pageSpecAnterior = propostaRepository
        .findFirstBySessaoIdOrderByCreatedAtDesc(sessao.getId())
        .map(AiProposta::getPageSpecJson)
        .orElse(null);
    return Optional.of(ContextoExecucao.from(job, pageSpecAnterior));
  }

  @Transactional
  public void avancar(UUID jobId, AiJobEtapa etapa, int progresso) {
    AiJob job = carregarParaAtualizar(jobId);
    if (!job.ativo() || job.getSessao().cancelada()) {
      cancelar(job);
      throw new AiJobCanceladoException();
    }
    job.atualizarProgresso(etapa, progresso);
    aiEventService.publicarJob(job);
  }

  @Transactional
  public ResultadoConclusao concluir(UUID jobId, ResultadoGeracao resultado) {
    AiJob job = carregarParaAtualizar(jobId);
    AiSessao sessao = job.getSessao();
    if (!job.ativo() || sessao.cancelada()) {
      cancelar(job);
      throw new AiJobCanceladoException();
    }

    propostaRepository
        .findFirstBySessaoIdAndStatusOrderByCreatedAtDesc(sessao.getId(), AiPropostaStatus.PENDENTE)
        .ifPresent(AiProposta::descartar);

    AiProposta proposta = propostaRepository.save(new AiProposta(
        sessao,
        job,
        resultado.tipo(),
        resultado.titulo(),
        resultado.slug(),
        resultado.codigoTela(),
        resultado.resumo(),
        resultado.conteudoHtml(),
        resultado.templateId(),
        resultado.templateVersao(),
        resultado.qualidadeJson(),
        resultado.pageSpecJson(),
        resultado.avisosGeracao(),
        resultado.promptVersao()));

    job.registrarTokens(resultado.tokensEntrada(), resultado.tokensSaida());
    job.sucesso();
    sessao.definirTemplateId(resultado.templateId());
    sessao.pronta();
    aiEventService.publicarJob(job);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_PROPOSTA,
        proposta.getId(),
        AiAuditoriaAcoes.PROPOSTA_GERADA,
        truncar(resultado.codigoTela() + " · " + resultado.titulo(), 200),
        null);
    return new ResultadoConclusao(proposta.getId(), job.latenciaMs());
  }

  @Transactional
  public void falhar(UUID jobId, String mensagemUsuario, String detalheTecnico) {
    AiJob job = carregarParaAtualizar(jobId);
    AiSessao sessao = job.getSessao();
    if (!job.ativo() || sessao.cancelada()) {
      cancelar(job);
      return;
    }

    UUID diagnosticoId = UUID.randomUUID();
    job.erro(mensagemUsuario, detalheTecnico, diagnosticoId);
    if (sessao.getStatus() == AiSessaoStatus.GERANDO) {
      sessao.erro();
    }
    aiEventService.publicarJob(job);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_SESSAO,
        sessao.getId(),
        AiAuditoriaAcoes.PROPOSTA_ERRO,
        truncar("diagnóstico " + diagnosticoId + " · " + mensagemUsuario, 200),
        null);
  }

  @Transactional
  public void cancelarSessao(UUID sessaoId) {
    jobRepository.findAllBySessaoIdAndStatusIn(sessaoId, STATUS_ATIVOS).forEach(this::cancelar);
  }

  @Transactional
  public List<UUID> recuperarAposReinicio() {
    List<UUID> pendentes = jobRepository.findAllByStatus(AiJobStatus.PENDENTE).stream()
        .map(AiJob::getId)
        .toList();
    for (AiJob interrompido : jobRepository.findAllByStatus(AiJobStatus.PROCESSANDO)) {
      UUID diagnosticoId = UUID.randomUUID();
      interrompido.erro(
          "A geração foi interrompida por uma reinicialização. Tente novamente.",
          "Job em PROCESSANDO encontrado durante a inicialização da aplicação.",
          diagnosticoId);
      if (interrompido.getSessao().getStatus() == AiSessaoStatus.GERANDO) {
        interrompido.getSessao().erro();
      }
      aiEventService.publicarJob(interrompido);
    }
    return pendentes;
  }

  @Transactional(readOnly = true)
  public Optional<AiJobResponse> atual(UUID sessaoId) {
    return jobRepository.findFirstBySessaoIdOrderByCreatedAtDesc(sessaoId).map(AiJobResponse::from);
  }

  @Transactional
  public void expirar(AiJob job, String motivo) {
    if (!job.ativo()) {
      return;
    }
    UUID diagnosticoId = UUID.randomUUID();
    job.erro(
        "A execução anterior foi interrompida. Uma nova tentativa pode ser iniciada.",
        motivo,
        diagnosticoId);
    if (job.getSessao().getStatus() == AiSessaoStatus.GERANDO) {
      job.getSessao().erro();
    }
    aiEventService.publicarJob(job);
  }

  public static List<AiJobStatus> statusAtivos() {
    return STATUS_ATIVOS;
  }

  private AiJob carregarParaAtualizar(UUID jobId) {
    return jobRepository.findByIdForUpdate(jobId)
        .orElseThrow(() -> new NotFoundException("Job de IA não encontrado."));
  }

  private void cancelar(AiJob job) {
    if (job.ativo()) {
      job.cancelar();
      aiEventService.publicarJob(job);
    }
  }

  private static String truncar(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }

  public record ContextoExecucao(
      UUID jobId,
      UUID sessaoId,
      AiObjetivo objetivo,
      String briefing,
      UUID projetoId,
      UUID moduloId,
      UUID clienteId,
      UUID paginaId,
      UUID templateId,
      List<String> componentesSelecionados,
      /** PageSpec da última proposta da sessão; base para aplicar as instruções de ajuste. */
      String pageSpecAnterior) {

    static ContextoExecucao from(AiJob job, String pageSpecAnterior) {
      AiSessao sessao = job.getSessao();
      return new ContextoExecucao(
          job.getId(),
          sessao.getId(),
          sessao.getObjetivo(),
          sessao.getBriefing(),
          sessao.getProjetoId(),
          sessao.getModuloId(),
          sessao.getClienteId(),
          sessao.getPaginaId(),
          sessao.getTemplateId(),
          sessao.getComponentesSelecionados(),
          pageSpecAnterior);
    }
  }

  public record ResultadoGeracao(
      AiPropostaTipo tipo,
      String titulo,
      String slug,
      String codigoTela,
      String resumo,
      String conteudoHtml,
      UUID templateId,
      Integer templateVersao,
      String qualidadeJson,
      String pageSpecJson,
      Integer tokensEntrada,
      Integer tokensSaida,
      List<String> avisosGeracao,
      String promptVersao) {}

  public record ResultadoConclusao(UUID propostaId, long latenciaMs) {}
}
