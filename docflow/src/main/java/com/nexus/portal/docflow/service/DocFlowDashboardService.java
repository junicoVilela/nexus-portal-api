package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.response.DocFlowDashboardResponse;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ModuloRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.repository.ProjetoRepository;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocFlowDashboardService {

  private static final int DIAS_PAGINA_DESATUALIZADA = 180;

  private final ClienteRepository clienteRepository;
  private final ProjetoRepository projetoRepository;
  private final ModuloRepository moduloRepository;
  private final PaginaRepository paginaRepository;
  private final PublicacaoRepository publicacaoRepository;

  @Transactional(readOnly = true)
  public DocFlowDashboardResponse resumo() {
    Map<StatusPagina, Long> paginasPorStatus = new EnumMap<>(StatusPagina.class);
    paginaRepository.contarPorStatusAgrupado()
        .forEach(item -> paginasPorStatus.put((StatusPagina) item[0], (Long) item[1]));

    long publicacoesSucesso = publicacaoRepository.countByStatus(StatusPublicacao.SUCESSO);
    long publicacoesErro = publicacaoRepository.countByStatus(StatusPublicacao.ERRO);
    long publicacoesFinalizadas = publicacoesSucesso + publicacoesErro;
    double taxaSucesso = publicacoesFinalizadas == 0
        ? 0
        : Math.round(publicacoesSucesso * 10_000.0 / publicacoesFinalizadas) / 100.0;

    long paginasPendentes = paginasPorStatus.getOrDefault(StatusPagina.RASCUNHO, 0L)
        + paginasPorStatus.getOrDefault(StatusPagina.EM_REVISAO, 0L)
        + paginasPorStatus.getOrDefault(StatusPagina.APROVADO, 0L);

    Map<String, Long> statusResponse = new LinkedHashMap<>();
    for (StatusPagina status : StatusPagina.values()) {
      long total = paginasPorStatus.getOrDefault(status, 0L);
      if (total > 0) {
        statusResponse.put(status.name(), total);
      }
    }

    return new DocFlowDashboardResponse(
        clienteRepository.countByAtivoTrue(),
        projetoRepository.countByAtivoTrue(),
        moduloRepository.countByAtivoTrue(),
        paginaRepository.countByAtivoTrue(),
        publicacaoRepository.count(),
        paginasPendentes,
        paginasPorStatus.getOrDefault(StatusPagina.EM_REVISAO, 0L),
        publicacaoRepository.countByStatus(StatusPublicacao.GERANDO),
        publicacoesErro,
        publicacaoRepository.countClientesAtivosSemPublicacaoSucesso(),
        paginaRepository.countSemResumoEditorial(),
        paginaRepository.countByStatusAndAtivoTrueAndPublishedAtBefore(
            StatusPagina.PUBLICADO,
            OffsetDateTime.now().minusDays(DIAS_PAGINA_DESATUALIZADA)),
        taxaSucesso,
        Map.copyOf(statusResponse));
  }
}
