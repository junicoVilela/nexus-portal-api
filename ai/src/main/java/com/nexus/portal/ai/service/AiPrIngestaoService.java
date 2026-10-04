package com.nexus.portal.ai.service;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.config.AiGithubProperties;
import com.nexus.portal.ai.config.AiGithubProperties.Repositorio;
import com.nexus.portal.ai.dto.request.AiAjustePaginaRequest;
import com.nexus.portal.ai.dto.request.CriarAiSessaoRequest;
import com.nexus.portal.ai.entity.AiFilaOrigem;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPrClassificacao;
import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge.PaginaAjuste;
import com.nexus.portal.ai.integration.github.AiGithubClient;
import com.nexus.portal.ai.integration.github.AiGithubClient.ArquivoPr;
import com.nexus.portal.ai.repository.AiPrEventoRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.shared.exception.NotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase C: PR mergeado → proposta na fila (AI-603…607).
 *
 * <pre>
 * receber   grava o PR uma vez (delivery-id e repositório+número únicos) e agenda o processamento
 * processar arquivos do PR → classificador → código de tela → página existente?
 *             sim, editável     → ajuste da Fase B sobre a página (patch)
 *             sim, publicada    → AGUARDANDO_RASCUNHO (o autor volta a rascunho e reprocessa)
 *             não               → sessão de página nova no projeto/módulo do repositório
 * </pre>
 *
 * Sessões nascem do usuário {@code system}; quem assume o item na fila vira o responsável. Cada
 * etapa que mexe no banco roda na própria transação: falha da IA não desfaz o registro do PR.
 */
@Service
public class AiPrIngestaoService {

  private static final Logger log = LoggerFactory.getLogger(AiPrIngestaoService.class);
  private static final int MAX_CORPO_BRIEFING = 4_000;
  private static final int MAX_INSTRUCAO_AJUSTE = 2_000;
  private static final Set<String> NAO_EDITAVEIS = Set.of("APROVADO", "PUBLICADO");

  private final AiPrEventoRepository eventoRepository;
  private final AiSessaoRepository sessaoRepository;
  private final AiGithubClient github;
  private final AiGithubProperties properties;
  private final DocFlowAiBridge docFlowAiBridge;
  private final AiSessaoService sessaoService;
  private final AiPropostaService propostaService;
  private final AiAjustePaginaService ajusteService;
  private final AuditoriaService auditoriaService;
  private final TransactionTemplate transacao;
  /** Próprio bean via proxy: {@code @Async} não vale em chamada interna. */
  private final ObjectProvider<AiPrIngestaoService> proxy;

  public AiPrIngestaoService(
      AiPrEventoRepository eventoRepository,
      AiSessaoRepository sessaoRepository,
      AiGithubClient github,
      AiGithubProperties properties,
      DocFlowAiBridge docFlowAiBridge,
      AiSessaoService sessaoService,
      AiPropostaService propostaService,
      AiAjustePaginaService ajusteService,
      AuditoriaService auditoriaService,
      PlatformTransactionManager transactionManager,
      ObjectProvider<AiPrIngestaoService> proxy) {
    this.eventoRepository = eventoRepository;
    this.sessaoRepository = sessaoRepository;
    this.github = github;
    this.properties = properties;
    this.docFlowAiBridge = docFlowAiBridge;
    this.sessaoService = sessaoService;
    this.propostaService = propostaService;
    this.ajusteService = ajusteService;
    this.auditoriaService = auditoriaService;
    this.transacao = new TransactionTemplate(transactionManager);
    this.proxy = proxy;
  }

  /** Dados do evento {@code pull_request} (closed + merged) que importam para a fila. */
  public record PullRequestMergeado(
      String deliveryId,
      String repositorio,
      int numero,
      String titulo,
      String corpo,
      List<String> rotulos,
      String url,
      String autor,
      String branchBase,
      String mergeSha,
      OffsetDateTime mergedAt) {}

  /**
   * Grava o PR e devolve o id do evento; vazio quando já foi recebido (reentrega do GitHub). O
   * processamento roda depois do commit, fora da requisição do webhook (o GitHub espera ~10 s).
   */
  public Optional<UUID> receber(PullRequestMergeado pr) {
    if (eventoRepository.existsByDeliveryId(pr.deliveryId())
        || eventoRepository.findByRepositorioAndNumeroPr(pr.repositorio(), pr.numero()).isPresent()) {
      return Optional.empty();
    }
    AiPrEvento evento = new AiPrEvento(pr.deliveryId(), pr.repositorio(), pr.numero(), pr.titulo(), pr.corpo(),
        pr.rotulos(), pr.url(), pr.autor(), pr.branchBase(), pr.mergeSha(), pr.mergedAt());
    if (properties.repositorio(pr.repositorio()).isEmpty()) {
      evento.ignorar("Repositório " + pr.repositorio() + " não está em nexus.ai.github.repositorios.");
    }
    try {
      transacao.executeWithoutResult(status -> {
        eventoRepository.saveAndFlush(evento);
        auditoriaService.registrar(AiAuditoriaAcoes.ENTIDADE_PR_EVENTO, evento.getId(),
            AiAuditoriaAcoes.PR_RECEBIDO, truncar(pr.repositorio() + "#" + pr.numero() + " · " + pr.titulo(), 200),
            null);
      });
    } catch (DataIntegrityViolationException corrida) {
      // Duas entregas simultâneas do mesmo PR: a outra venceu.
      return Optional.empty();
    }
    return Optional.of(evento.getId());
  }

  /** Classifica o PR e abre a sessão. Seguro para reprocessar itens em erro ou aguardando rascunho. */
  @Async
  public void processarDepois(UUID eventoId) {
    processar(eventoId);
  }

  public void processar(UUID eventoId) {
    AiPrEvento evento = eventoRepository.findById(eventoId)
        .orElseThrow(() -> new NotFoundException("Item da fila não encontrado."));
    if (!evento.podeReprocessar()) {
      return;
    }
    if (evento.getOrigem() == AiFilaOrigem.RELEASE) {
      processarRelease(eventoId, evento);
      return;
    }
    Optional<Repositorio> repositorio = properties.repositorio(evento.getRepositorio());
    if (repositorio.isEmpty()) {
      atualizar(eventoId, e -> e.ignorar("Repositório " + e.getRepositorio() + " não está configurado."));
      return;
    }
    try {
      List<ArquivoPr> arquivos = github.arquivos(evento.getRepositorio(), evento.getNumeroPr(),
          properties.maxArquivos());
      AiPrClassificador.Resultado resultado = AiPrClassificador.classificar(evento.getTitulo(), evento.getCorpo(),
          evento.getRotulos(), arquivos, properties.caminhosTela(), properties.rotuloIgnorar());
      Optional<PaginaAjuste> pagina = resultado.codigosTela().stream()
          .map(docFlowAiBridge::buscarPaginaPorCodigoTela)
          .flatMap(Optional::stream)
          .findFirst();
      String codigo = pagina.map(PaginaAjuste::codigoTela).orElse(resultado.codigoSugerido());
      atualizar(eventoId, e -> e.classificar(resultado.classificacao(), codigo));
      if (!resultado.classificacao().geraProposta()) {
        atualizar(eventoId, e -> e.ignorar(resultado.motivo()));
        return;
      }
      if (pagina.isPresent()) {
        abrirAjuste(eventoId, evento, pagina.get(), resultado);
      } else {
        abrirPaginaNova(eventoId, evento, repositorio.get(), arquivos, resultado, codigo);
      }
    } catch (RuntimeException ex) {
      log.warn("ai.pr.falha evento={} pr={}#{}: {}", eventoId, evento.getRepositorio(), evento.getNumeroPr(),
          ex.getMessage());
      atualizar(eventoId, e -> e.falhar(mensagem(ex)));
    }
  }

  /**
   * INT-303: "Gerar ajuste" num item de release — a instrução é o resumo e os itens da release.
   * Página publicada aguarda voltar a rascunho, como nos PRs.
   */
  private void processarRelease(UUID eventoId, AiPrEvento evento) {
    try {
      Optional<PaginaAjuste> pagina = docFlowAiBridge.buscarPaginaPorCodigoTela(evento.getCodigoTela());
      if (pagina.isEmpty()) {
        atualizar(eventoId, e -> e.ignorar("A tela " + e.getCodigoTela() + " não tem página no DocFlow."));
        return;
      }
      String instrucao = truncar("Atualize esta página para refletir a release " + evento.getRepositorio()
          + " (\"" + evento.getTitulo() + "\"). Mudanças da release:\n" + textoOuVazio(evento.getCorpo()),
          MAX_INSTRUCAO_AJUSTE);
      abrirAjuste(eventoId, pagina.get(), instrucao);
    } catch (RuntimeException ex) {
      log.warn("ai.release.falha evento={} tela={}: {}", eventoId, evento.getCodigoTela(), ex.getMessage());
      atualizar(eventoId, e -> e.falhar(mensagem(ex)));
    }
  }

  /** Agenda o processamento para depois do commit da transação atual (ou já, sem transação). */
  public void agendar(UUID eventoId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          proxy.getObject().processarDepois(eventoId);
        }
      });
    } else {
      proxy.getObject().processarDepois(eventoId);
    }
  }

  private void abrirAjuste(UUID eventoId, AiPrEvento evento, PaginaAjuste pagina,
      AiPrClassificador.Resultado resultado) {
    String instrucao = truncar("Atualize esta página para refletir o PR " + evento.getRepositorio() + "#"
        + evento.getNumeroPr() + " (mergeado): \"" + evento.getTitulo() + "\".\n"
        + "Arquivos de tela alterados: " + String.join(", ", resultado.arquivosTela()) + ".\n"
        + "Descrição do PR:\n" + textoOuVazio(evento.getCorpo()), MAX_INSTRUCAO_AJUSTE);
    abrirAjuste(eventoId, pagina, instrucao);
  }

  private void abrirAjuste(UUID eventoId, PaginaAjuste pagina, String instrucao) {
    if (NAO_EDITAVEIS.contains(pagina.status())) {
      atualizar(eventoId, e -> e.aguardarRascunho(pagina.id(), "Página " + pagina.codigoTela() + " "
          + ("PUBLICADO".equals(pagina.status()) ? "publicada" : "aprovada")
          + ": volte para rascunho e reprocesse para gerar o ajuste."));
      return;
    }
    transacao.executeWithoutResult(status -> {
      var resposta = ajusteService.pedir(pagina.id(),
          new AiAjustePaginaRequest(instrucao, null, pagina.version()), null);
      eventoRepository.findById(eventoId).orElseThrow().emFila(resposta.sessaoId(), pagina.id(),
          "Ajuste da página " + pagina.codigoTela() + " em geração.");
    });
  }

  private void abrirPaginaNova(UUID eventoId, AiPrEvento evento, Repositorio repositorio, List<ArquivoPr> arquivos,
      AiPrClassificador.Resultado resultado, String codigo) {
    if (repositorio.projetoId() == null || repositorio.moduloId() == null) {
      atualizar(eventoId, e -> e.falhar("Configure projeto-id e modulo-id de " + repositorio.nome()
          + " para gerar páginas novas."));
      return;
    }
    String briefing = briefingPaginaNova(evento, arquivos, resultado, codigo);
    transacao.executeWithoutResult(status -> {
      var sessao = sessaoService.criar(new CriarAiSessaoRequest(AiObjetivo.CRIAR_PAGINA, briefing,
          repositorio.projetoId(), repositorio.moduloId(), null, null, null), null);
      // A triagem pode pedir respostas; o PR não responde: gera com o que há e o autor revisa na fila.
      sessaoRepository.findById(sessao.id())
          .filter(s -> s.getStatus() == AiSessaoStatus.AGUARDANDO_USUARIO)
          .ifPresent(s -> s.prontaParaGerar());
      propostaService.gerar(sessao.id(), null, null);
      eventoRepository.findById(eventoId).orElseThrow().emFila(sessao.id(), null,
          "Página nova em geração" + (codigo == null ? "." : " (código sugerido " + codigo + ")."));
    });
  }

  String briefingPaginaNova(AiPrEvento evento, List<ArquivoPr> arquivos, AiPrClassificador.Resultado resultado,
      String codigo) {
    StringBuilder briefing = new StringBuilder()
        .append("Documente a tela entregue no PR ").append(evento.getRepositorio()).append('#')
        .append(evento.getNumeroPr()).append(" (mergeado em ").append(evento.getBranchBase()).append("): ")
        .append(evento.getTitulo()).append(".\n");
    if (codigo != null) {
      briefing.append("Código da tela: ").append(codigo).append(".\n");
    }
    briefing.append("\nDescrição do PR:\n").append(truncar(textoOuVazio(evento.getCorpo()), MAX_CORPO_BRIEFING))
        .append("\n\nArquivos de tela:\n")
        .append(resultado.arquivosTela().stream().map(c -> "- " + c).collect(Collectors.joining("\n")))
        .append("\n\nTrechos alterados (diff):\n");
    int restante = properties.maxCaracteresPatch();
    for (ArquivoPr arquivo : arquivos) {
      if (arquivo.patch() == null || !resultado.arquivosTela().contains(arquivo.caminho()) || restante <= 0) {
        continue;
      }
      String trecho = truncar(arquivo.patch(), restante);
      briefing.append("--- ").append(arquivo.caminho()).append('\n').append(trecho).append('\n');
      restante -= trecho.length();
    }
    return briefing.toString();
  }

  private void atualizar(UUID eventoId, Consumer<AiPrEvento> mudanca) {
    transacao.executeWithoutResult(status -> mudanca.accept(eventoRepository.findById(eventoId).orElseThrow()));
  }

  private static String mensagem(RuntimeException ex) {
    if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
      return rse.getReason();
    }
    return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
  }

  private static String textoOuVazio(String texto) {
    return texto == null || texto.isBlank() ? "(sem descrição)" : texto.strip();
  }

  private static String truncar(String texto, int max) {
    return texto.length() <= max ? texto : texto.substring(0, Math.max(0, max - 1)) + "…";
  }
}
