package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ModuloRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.repository.ProjetoRepository;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocFlowDashboardServiceTest {

  @Mock ClienteRepository clienteRepository;
  @Mock ProjetoRepository projetoRepository;
  @Mock ModuloRepository moduloRepository;
  @Mock PaginaRepository paginaRepository;
  @Mock PublicacaoRepository publicacaoRepository;
  @InjectMocks DocFlowDashboardService service;

  @Test
  void resumo_consolidaIndicadoresOperacionaisEEditoriais() {
    when(clienteRepository.countByAtivoTrue()).thenReturn(3L);
    when(projetoRepository.countByAtivoTrue()).thenReturn(2L);
    when(moduloRepository.countByAtivoTrue()).thenReturn(5L);
    when(paginaRepository.countByAtivoTrue()).thenReturn(20L);
    when(publicacaoRepository.count()).thenReturn(10L);
    when(paginaRepository.contarPorStatusAgrupado()).thenReturn(List.of(
        new Object[] {StatusPagina.RASCUNHO, 4L},
        new Object[] {StatusPagina.EM_REVISAO, 2L},
        new Object[] {StatusPagina.APROVADO, 1L},
        new Object[] {StatusPagina.PUBLICADO, 8L}));
    when(publicacaoRepository.countByStatus(StatusPublicacao.GERANDO)).thenReturn(1L);
    when(publicacaoRepository.countByStatus(StatusPublicacao.SUCESSO)).thenReturn(6L);
    when(publicacaoRepository.countByStatus(StatusPublicacao.ERRO)).thenReturn(2L);
    when(publicacaoRepository.countClientesAtivosSemPublicacaoSucesso()).thenReturn(2L);
    when(paginaRepository.countSemResumoEditorial()).thenReturn(3L);
    when(paginaRepository.countByStatusAndAtivoTrueAndPublishedAtBefore(
        org.mockito.ArgumentMatchers.eq(StatusPagina.PUBLICADO), any(OffsetDateTime.class)))
        .thenReturn(4L);

    var resumo = service.resumo();

    assertThat(resumo.totalClientes()).isEqualTo(3);
    assertThat(resumo.totalPaginas()).isEqualTo(20);
    assertThat(resumo.paginasPendentes()).isEqualTo(7);
    assertThat(resumo.paginasEmRevisao()).isEqualTo(2);
    assertThat(resumo.taxaSucessoPublicacoes()).isEqualTo(75.0);
    assertThat(resumo.paginasPorStatus()).containsEntry("EM_REVISAO", 2L);
    assertThat(resumo.paginasSemResumo()).isEqualTo(3);
    assertThat(resumo.paginasDesatualizadas()).isEqualTo(4);
  }
}
