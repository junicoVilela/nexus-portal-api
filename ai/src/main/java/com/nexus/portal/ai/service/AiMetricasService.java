package com.nexus.portal.ai.service;

import com.nexus.portal.ai.dto.response.AiMetricasResponse;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Ajustes;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.AlteracoesPosAceite;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.AvisoFrequente;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Contagem;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Geracao;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Manual;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.PorPrompt;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.PorTipoOperacao;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Rejeicao;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.RejeicaoPorCategoria;
import com.nexus.portal.ai.entity.AiCategoriaRejeicao;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiManualPergunta;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge.PaginaAjuste;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiManualPerguntaRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrega o que já é gravado em {@code tb_ai_job} e {@code tb_ai_proposta} para dizer se uma
 * mudança de prompt melhorou ou piorou: aceite por {@code prompt_versao}, avisos de fallback,
 * motivos de rejeição, aceite parcial dos ajustes, latência e tokens.
 */
@Service
public class AiMetricasService {

  static final String SEM_VERSAO = "sem versão (antes do V40)";
  private static final int MAX_DIAS = 365;
  private static final int TOP_AVISOS = 8;
  private static final int TOP_REJEICOES = 10;
  /** Teto de páginas lidas por cálculo de "texto mantido" (as mais recentes). */
  static final int MAX_AMOSTRAS_TEXTO = 200;
  /** Abaixo disso o autor reescreveu a maior parte do texto da IA. */
  static final double LIMIAR_REESCRITA = 0.5;

  private final AiJobRepository jobRepository;
  private final AiPropostaRepository propostaRepository;
  private final AiPagePatchService pagePatchService;
  private final DocFlowAiBridge docFlowAiBridge;
  private final AiManualPerguntaRepository manualPerguntaRepository;

  public AiMetricasService(
      AiJobRepository jobRepository,
      AiPropostaRepository propostaRepository,
      AiPagePatchService pagePatchService,
      DocFlowAiBridge docFlowAiBridge,
      AiManualPerguntaRepository manualPerguntaRepository) {
    this.jobRepository = jobRepository;
    this.propostaRepository = propostaRepository;
    this.pagePatchService = pagePatchService;
    this.docFlowAiBridge = docFlowAiBridge;
    this.manualPerguntaRepository = manualPerguntaRepository;
  }

  @Transactional(readOnly = true)
  public AiMetricasResponse calcular(int dias) {
    if (dias < 1 || dias > MAX_DIAS) {
      throw new BusinessException("Período deve ter entre 1 e " + MAX_DIAS + " dias.");
    }
    OffsetDateTime desde = OffsetDateTime.now().minusDays(dias);
    List<AiJob> jobs = jobRepository.findByCreatedAtAfter(desde);
    List<AiProposta> propostas = propostaRepository.findByCreatedAtAfter(desde);
    Map<AiProposta, PaginaAjuste> paginas = paginasAceitas(propostas);
    Map<AiProposta, Double> mantido = textoMantido(paginas);
    return new AiMetricasResponse(
        dias,
        desde,
        geracao(jobs),
        porPrompt(propostas, mantido),
        ajustes(propostas),
        avisosFrequentes(propostas),
        rejeicoesRecentes(propostas),
        rejeicoesPorCategoria(propostas),
        alteracoesPosAceite(paginas, mantido),
        manual(manualPerguntaRepository.findByCreatedAtAfter(desde)));
  }

  private static Geracao geracao(List<AiJob> jobs) {
    List<Long> latencias = jobs.stream()
        .filter(job -> job.getStatus() == AiJobStatus.SUCESSO)
        .map(AiJob::latenciaMs)
        .filter(latencia -> latencia > 0)
        .sorted()
        .toList();
    return new Geracao(
        jobs.size(),
        contar(jobs, job -> job.getStatus() == AiJobStatus.SUCESSO),
        contar(jobs, job -> job.getStatus() == AiJobStatus.ERRO),
        contar(jobs, job -> job.getStatus() == AiJobStatus.CANCELADO),
        percentil(latencias, 50),
        percentil(latencias, 90),
        jobs.stream().mapToLong(job -> valor(job.getTokensEntrada())).sum(),
        jobs.stream().mapToLong(job -> valor(job.getTokensSaida())).sum());
  }

  /**
   * Página de hoje de cada proposta aceita (as {@link #MAX_AMOSTRAS_TEXTO} mais recentes). Página
   * removida ou inacessível fica fora da amostra.
   */
  private Map<AiProposta, PaginaAjuste> paginasAceitas(List<AiProposta> propostas) {
    Map<UUID, PaginaAjuste> cache = new HashMap<>();
    Map<AiProposta, PaginaAjuste> resultado = new IdentityHashMap<>();
    propostas.stream()
        .filter(p -> p.getStatus() == AiPropostaStatus.ACEITA && p.getPaginaId() != null)
        .sorted(Comparator.comparing(AiProposta::getUpdatedAt).reversed())
        .limit(MAX_AMOSTRAS_TEXTO)
        .forEach(proposta -> {
          PaginaAjuste pagina = cache.computeIfAbsent(proposta.getPaginaId(), this::buscarPagina);
          if (pagina != null) {
            resultado.put(proposta, pagina);
          }
        });
    return resultado;
  }

  private PaginaAjuste buscarPagina(UUID paginaId) {
    try {
      return docFlowAiBridge.buscarPaginaParaAjuste(paginaId);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  /** Fração do texto de cada proposta aceita que continua na página hoje. */
  private static Map<AiProposta, Double> textoMantido(Map<AiProposta, PaginaAjuste> paginas) {
    Map<AiProposta, Double> resultado = new IdentityHashMap<>();
    paginas.forEach((proposta, pagina) -> {
      Double fracao = AiTextoMantido.fracao(proposta.getConteudoHtml(), pagina.conteudoHtml());
      if (fracao != null) {
        resultado.put(proposta, fracao);
      }
    });
    return resultado;
  }

  /** Para propostas de página nova; ajustes (Fase B) já medem o aceite por operação. */
  private static AlteracoesPosAceite alteracoesPosAceite(
      Map<AiProposta, PaginaAjuste> paginas, Map<AiProposta, Double> mantido) {
    List<Map.Entry<AiProposta, PaginaAjuste>> novas = paginas.entrySet().stream()
        .filter(e -> e.getKey().getTipo() == AiPropostaTipo.NOVA)
        .toList();
    return new AlteracoesPosAceite(
        novas.size(),
        contar(novas, e -> mudou(e.getKey().getTitulo(), e.getValue().titulo())),
        contar(novas, e -> mudou(e.getKey().getResumo(), e.getValue().resumo())),
        contar(novas, e -> mudou(e.getKey().getCodigoTela(), e.getValue().codigoTela())),
        contar(novas, e -> {
          Double fracao = mantido.get(e.getKey());
          return fracao != null && fracao < LIMIAR_REESCRITA;
        }));
  }

  /** Ignora espaços nas pontas e repetidos: só conta o que o autor de fato reescreveu. */
  static boolean mudou(String proposto, String atual) {
    return !normalizar(proposto).equals(normalizar(atual));
  }

  private static String normalizar(String texto) {
    return texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
  }

  private static List<PorPrompt> porPrompt(List<AiProposta> propostas, Map<AiProposta, Double> mantido) {
    Map<String, List<AiProposta>> grupos = propostas.stream().collect(Collectors.groupingBy(
        proposta -> proposta.getPromptVersao() == null ? SEM_VERSAO : proposta.getPromptVersao(),
        LinkedHashMap::new,
        Collectors.toList()));
    return grupos.entrySet().stream()
        .map(grupo -> {
          List<AiProposta> lista = grupo.getValue();
          long aceitas = contar(lista, p -> p.getStatus() == AiPropostaStatus.ACEITA);
          long rejeitadas = contar(lista, p -> p.getStatus() == AiPropostaStatus.REJEITADA);
          long regeneradas = contar(lista, p -> p.getStatus() == AiPropostaStatus.DESCARTADA);
          List<Double> amostras = lista.stream().map(mantido::get).filter(Objects::nonNull).toList();
          return new PorPrompt(
              grupo.getKey(),
              lista.size(),
              aceitas,
              rejeitadas,
              regeneradas,
              contar(lista, p -> p.getStatus() == AiPropostaStatus.PENDENTE),
              contar(lista, p -> p.getAvisosGeracao() != null && !p.getAvisosGeracao().isEmpty()),
              taxa(aceitas, aceitas + rejeitadas + regeneradas),
              amostras.isEmpty()
                  ? null
                  : amostras.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
              amostras.size(),
              lista.stream()
                  .filter(p -> p.getStatus() == AiPropostaStatus.REJEITADA && p.getCategoriaRejeicao() != null)
                  .collect(Collectors.groupingBy(
                      p -> p.getCategoriaRejeicao().name(), TreeMap::new, Collectors.counting())));
        })
        .sorted(Comparator.comparing(PorPrompt::promptVersao).reversed())
        .toList();
  }

  private Ajustes ajustes(List<AiProposta> propostas) {
    Map<String, long[]> porTipo = new LinkedHashMap<>();
    for (AiPagePatch.Tipo tipo : AiPagePatch.Tipo.values()) {
      porTipo.put(tipo.name(), new long[2]);
    }
    long aplicados = 0;
    for (AiProposta proposta : propostas) {
      if (proposta.getTipo() != AiPropostaTipo.ATUALIZACAO
          || proposta.getStatus() != AiPropostaStatus.ACEITA
          || proposta.getPatchJson() == null) {
        continue;
      }
      aplicados++;
      Set<String> aceitas = proposta.getOperacoesAceitas() == null
          ? Set.of()
          : Set.copyOf(proposta.getOperacoesAceitas());
      for (AiPagePatch.Operacao operacao : pagePatchService.ler(proposta.getPatchJson()).operacoes()) {
        long[] contagem = porTipo.get(operacao.tipo().name());
        contagem[0]++;
        if (aceitas.contains(operacao.id())) {
          contagem[1]++;
        }
      }
    }
    long propostasOps = porTipo.values().stream().mapToLong(c -> c[0]).sum();
    long aceitasOps = porTipo.values().stream().mapToLong(c -> c[1]).sum();
    return new Ajustes(
        aplicados,
        propostasOps,
        aceitasOps,
        taxa(aceitasOps, propostasOps),
        porTipo.entrySet().stream()
            .map(e -> new PorTipoOperacao(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList());
  }

  private static List<AvisoFrequente> avisosFrequentes(List<AiProposta> propostas) {
    return propostas.stream()
        .filter(proposta -> proposta.getAvisosGeracao() != null)
        .flatMap(proposta -> proposta.getAvisosGeracao().stream())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
        .limit(TOP_AVISOS)
        .map(e -> new AvisoFrequente(e.getKey(), e.getValue()))
        .toList();
  }

  private static List<Rejeicao> rejeicoesRecentes(List<AiProposta> propostas) {
    return propostas.stream()
        .filter(p -> p.getStatus() == AiPropostaStatus.REJEITADA)
        .filter(p -> p.getMotivoRejeicao() != null || p.getCategoriaRejeicao() != null)
        .sorted(Comparator.comparing(AiProposta::getUpdatedAt).reversed())
        .limit(TOP_REJEICOES)
        .map(p -> new Rejeicao(
            p.getCategoriaRejeicao() == null ? null : p.getCategoriaRejeicao().name(),
            p.getMotivoRejeicao(),
            p.getPromptVersao() == null ? SEM_VERSAO : p.getPromptVersao(),
            p.getUpdatedAt()))
        .toList();
  }

  /** Todas as categorias, na ordem do enum, inclusive as zeradas (o painel mostra a escala). */
  private static List<RejeicaoPorCategoria> rejeicoesPorCategoria(List<AiProposta> propostas) {
    Map<AiCategoriaRejeicao, Long> contagem = propostas.stream()
        .filter(p -> p.getStatus() == AiPropostaStatus.REJEITADA && p.getCategoriaRejeicao() != null)
        .collect(Collectors.groupingBy(AiProposta::getCategoriaRejeicao, Collectors.counting()));
    return Arrays.stream(AiCategoriaRejeicao.values())
        .map(c -> new RejeicaoPorCategoria(c.name(), c.rotulo(), contagem.getOrDefault(c, 0L)))
        .toList();
  }

  private static Manual manual(List<AiManualPergunta> perguntas) {
    long naoSei = contar(perguntas, p -> p.getModo() == AiManualPergunta.Modo.NAO_SEI);
    return new Manual(
        perguntas.size(),
        contar(perguntas, p -> p.getModo() == AiManualPergunta.Modo.IA),
        contar(perguntas, p -> p.getModo() == AiManualPergunta.Modo.TRECHOS),
        naoSei,
        taxa(naoSei, perguntas.size()),
        topo(perguntas.stream()
            .filter(p -> p.getModo() == AiManualPergunta.Modo.NAO_SEI)
            .map(p -> p.getPergunta().strip().toLowerCase(Locale.ROOT))),
        topo(perguntas.stream().flatMap(p -> p.getCodigosCitados().stream())));
  }

  private static List<Contagem> topo(java.util.stream.Stream<String> valores) {
    return valores.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
        .limit(TOP_AVISOS)
        .map(e -> new Contagem(e.getKey(), e.getValue()))
        .toList();
  }

  /** Percentil pelo método do valor mais próximo; nulo sem amostras. */
  static Long percentil(List<Long> ordenados, int p) {
    if (ordenados.isEmpty()) {
      return null;
    }
    int indice = (int) Math.ceil(p / 100.0 * ordenados.size()) - 1;
    return ordenados.get(Math.max(0, Math.min(indice, ordenados.size() - 1)));
  }

  private static Double taxa(long parte, long total) {
    return total == 0 ? null : (double) parte / total;
  }

  private static <T> long contar(List<T> itens, Predicate<T> filtro) {
    return itens.stream().filter(filtro).count();
  }

  private static long valor(Integer numero) {
    return numero == null ? 0 : numero;
  }
}
