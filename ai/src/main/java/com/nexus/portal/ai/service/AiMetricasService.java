package com.nexus.portal.ai.service;

import com.nexus.portal.ai.dto.response.AiMetricasResponse;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Ajustes;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.AvisoFrequente;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Geracao;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.PorPrompt;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.PorTipoOperacao;
import com.nexus.portal.ai.dto.response.AiMetricasResponse.Rejeicao;
import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiJobRepository;
import com.nexus.portal.ai.repository.AiPropostaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

  private final AiJobRepository jobRepository;
  private final AiPropostaRepository propostaRepository;
  private final AiPagePatchService pagePatchService;
  private final DocFlowAiBridge docFlowAiBridge;

  public AiMetricasService(
      AiJobRepository jobRepository,
      AiPropostaRepository propostaRepository,
      AiPagePatchService pagePatchService,
      DocFlowAiBridge docFlowAiBridge) {
    this.jobRepository = jobRepository;
    this.propostaRepository = propostaRepository;
    this.pagePatchService = pagePatchService;
    this.docFlowAiBridge = docFlowAiBridge;
  }

  @Transactional(readOnly = true)
  public AiMetricasResponse calcular(int dias) {
    if (dias < 1 || dias > MAX_DIAS) {
      throw new BusinessException("Período deve ter entre 1 e " + MAX_DIAS + " dias.");
    }
    OffsetDateTime desde = OffsetDateTime.now().minusDays(dias);
    List<AiJob> jobs = jobRepository.findByCreatedAtAfter(desde);
    List<AiProposta> propostas = propostaRepository.findByCreatedAtAfter(desde);
    return new AiMetricasResponse(
        dias,
        desde,
        geracao(jobs),
        porPrompt(propostas, textoMantido(propostas)),
        ajustes(propostas),
        avisosFrequentes(propostas),
        rejeicoesRecentes(propostas));
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
   * Fração do texto de cada proposta aceita que continua na página hoje. Página removida ou
   * inacessível fica fora da amostra.
   */
  private Map<AiProposta, Double> textoMantido(List<AiProposta> propostas) {
    Map<UUID, String> paginas = new HashMap<>();
    Map<AiProposta, Double> resultado = new IdentityHashMap<>();
    propostas.stream()
        .filter(p -> p.getStatus() == AiPropostaStatus.ACEITA && p.getPaginaId() != null)
        .sorted(Comparator.comparing(AiProposta::getUpdatedAt).reversed())
        .limit(MAX_AMOSTRAS_TEXTO)
        .forEach(proposta -> {
          String html = paginas.computeIfAbsent(proposta.getPaginaId(), this::conteudoPagina);
          Double fracao = html == null ? null : AiTextoMantido.fracao(proposta.getConteudoHtml(), html);
          if (fracao != null) {
            resultado.put(proposta, fracao);
          }
        });
    return resultado;
  }

  private String conteudoPagina(UUID paginaId) {
    try {
      return docFlowAiBridge.buscarPaginaParaAjuste(paginaId).conteudoHtml();
    } catch (RuntimeException ex) {
      return null;
    }
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
              amostras.size());
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
        .filter(p -> p.getStatus() == AiPropostaStatus.REJEITADA && p.getMotivoRejeicao() != null)
        .sorted(Comparator.comparing(AiProposta::getUpdatedAt).reversed())
        .limit(TOP_REJEICOES)
        .map(p -> new Rejeicao(
            p.getMotivoRejeicao(),
            p.getPromptVersao() == null ? SEM_VERSAO : p.getPromptVersao(),
            p.getUpdatedAt()))
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
