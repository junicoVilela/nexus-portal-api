package com.nexus.portal.docflow.dto.response;

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
    Map<String, Long> paginasPorStatus) {
}
