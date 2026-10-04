package com.nexus.portal.ai.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Painel de qualidade da IA. {@code taxaAceite} = aceitas / (aceitas + rejeitadas + regeneradas):
 * só conta propostas sobre as quais o autor já decidiu; regenerar conta como "não serviu".
 */
public record AiMetricasResponse(
    int periodoDias,
    OffsetDateTime desde,
    Geracao geracao,
    List<PorPrompt> porPrompt,
    Ajustes ajustes,
    List<AvisoFrequente> avisosFrequentes,
    List<Rejeicao> rejeicoesRecentes,
    List<RejeicaoPorCategoria> rejeicoesPorCategoria,
    AlteracoesPosAceite alteracoesPosAceite,
    Manual manual) {

  public record Geracao(
      long jobs,
      long sucesso,
      long erro,
      long cancelados,
      Long latenciaP50Ms,
      Long latenciaP90Ms,
      long tokensEntrada,
      long tokensSaida) {}

  public record PorPrompt(
      String promptVersao,
      long propostas,
      long aceitas,
      long rejeitadas,
      long regeneradas,
      long pendentes,
      long comAvisos,
      Double taxaAceite,
      /** Média do texto da IA ainda presente nas páginas salvas (0–1); nulo sem amostra. */
      Double textoMantido,
      /** Quantas propostas aceitas com página entraram na média de texto mantido. */
      long amostrasTextoMantido,
      /** Rejeições por {@code AiCategoriaRejeicao} (só as categorias que ocorreram). */
      Map<String, Long> rejeicoesPorCategoria) {}

  /** Fase B: aceite parcial por operação nos ajustes aplicados. */
  public record Ajustes(
      long aplicados,
      long operacoesPropostas,
      long operacoesAceitas,
      Double taxaAceiteOperacoes,
      List<PorTipoOperacao> porTipo) {}

  public record PorTipoOperacao(String tipo, long propostas, long aceitas) {}

  public record AvisoFrequente(String aviso, long ocorrencias) {}

  /** {@code categoria} e {@code motivo} são opcionais; ao menos um vem preenchido. */
  public record Rejeicao(String categoria, String motivo, String promptVersao, OffsetDateTime em) {}

  public record RejeicaoPorCategoria(String categoria, String rotulo, long total) {}

  /**
   * O que o autor mudou depois de aceitar: compara a proposta com a página de hoje. Cada campo
   * conta propostas em que ele mudou; {@code conteudoReescrito} = menos da metade do texto da IA
   * continua na página.
   */
  /**
   * Perguntas ao manual publicado (Onda E). {@code semResposta}: perguntas que terminaram em "não
   * sei" — lacunas do manual; {@code telasMaisCitadas}: o que os leitores mais procuram.
   */
  public record Manual(
      long perguntas,
      long comIa,
      long soTrechos,
      long naoSei,
      Double taxaNaoSei,
      List<Contagem> semResposta,
      List<Contagem> telasMaisCitadas) {}

  public record Contagem(String valor, long ocorrencias) {}

  public record AlteracoesPosAceite(
      long amostras,
      long tituloAlterado,
      long resumoAlterado,
      long codigoTelaAlterado,
      long conteudoReescrito) {}
}
