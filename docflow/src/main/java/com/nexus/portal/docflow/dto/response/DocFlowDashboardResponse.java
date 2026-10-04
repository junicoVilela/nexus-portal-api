package com.nexus.portal.docflow.dto.response;

import java.util.List;
import java.util.Map;

public record DocFlowDashboardResponse(
    long totalClientes,
    long totalProjetos,
    long totalModulos,
    long totalPaginas,
    long totalPublicacoes,
    long paginasPendentes,
    long paginasEmRevisao,
    long publicacoesGerando,
    long publicacoesComErro,
    long clientesSemPublicacao,
    long paginasSemResumo,
    long paginasDesatualizadas,
    double taxaSucessoPublicacoes,
    Map<String, Long> paginasPorStatus,
    /** INT-302: telas alteradas por release depois da última publicação. */
    long paginasDesatualizadasPorRelease,
    /** INT-606: o que os leitores procuraram e não acharam (30 dias). */
    Lacunas lacunas) {

  public record Lacunas(long buscas, long buscasSemResultado, List<Termo> termosSemResultado) {}

  public record Termo(String termo, long ocorrencias) {}
}
