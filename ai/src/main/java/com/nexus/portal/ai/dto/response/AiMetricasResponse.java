package com.nexus.portal.ai.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

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
    List<Rejeicao> rejeicoesRecentes) {

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
      Double taxaAceite) {}

  /** Fase B: aceite parcial por operação nos ajustes aplicados. */
  public record Ajustes(
      long aplicados,
      long operacoesPropostas,
      long operacoesAceitas,
      Double taxaAceiteOperacoes,
      List<PorTipoOperacao> porTipo) {}

  public record PorTipoOperacao(String tipo, long propostas, long aceitas) {}

  public record AvisoFrequente(String aviso, long ocorrencias) {}

  public record Rejeicao(String motivo, String promptVersao, OffsetDateTime em) {}
}
