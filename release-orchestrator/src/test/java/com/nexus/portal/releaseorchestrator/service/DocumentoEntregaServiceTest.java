package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.CategoriaItem;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseItem;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.entity.VisibilidadeItem;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseItemRepository;
import com.nexus.portal.shared.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoEntregaServiceTest {

  @Mock OrchestratorEntregaRepository entregaRepository;
  @Mock OrchestratorEntregaModuloRepository entregaModuloRepository;
  @Mock ReleaseItemRepository releaseItemRepository;

  DocumentoEntregaService service;
  MarkdownPdfRenderer renderer;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  Entrega entrega;
  ModuloProduto modPortal;
  final UUID entregaId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    renderer = new MarkdownPdfRenderer();
    service = new DocumentoEntregaService(entregaRepository, entregaModuloRepository,
        releaseItemRepository, renderer);

    cliente = new Cliente("ACME LTDA", "ACME", AmbientePadrao.PROD);
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    release = new Release(produto, "1.5.0", "Release Junho",
        TipoRelease.MINOR, ReleaseStatus.PUBLICADA,
        LocalDate.of(2026, 6, 30), null, "Resumo da release.", null);
    entrega = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null,
        "Janela noturna sábado.");
    modPortal = new ModuloProduto(produto, "nexus-portal", "Portal", TipoModulo.WEB,
        false, true, 0, null);
  }

  @Test
  void gerar_produzPdfValidoComMagicBytes() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(release.getId()))
        .thenReturn(List.of(item("SSO", "Login único", VisibilidadeItem.TODOS)));
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(em(modPortal, "1.4.0", "1.5.0", true)));

    byte[] pdf = service.gerar(entregaId);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void gerar_filtraItensFora_CLIENTE() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(release.getId()))
        .thenReturn(List.of(
            item("SSO", "Login único", VisibilidadeItem.TODOS),
            item("Refactor X", "Detalhe técnico", VisibilidadeItem.TECNICO),
            item("Runbook", "Suporte", VisibilidadeItem.SUPORTE)));
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of());

    byte[] pdf = service.gerar(entregaId);
    assertThat(pdf).isNotEmpty();
  }

  @Test
  void gerar_filtraModulosNaoSelecionados() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(release.getId()))
        .thenReturn(List.of());
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(
            em(modPortal, "1.4.0", "1.5.0", true),
            em(modPortal, "1.4.0", "1.5.0", false)));  // não selecionado

    byte[] pdf = service.gerar(entregaId);
    assertThat(pdf).isNotEmpty();
  }

  @Test
  void gerar_entregaSemItensProduzPdfComCabecalhoEModulos() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(release.getId()))
        .thenReturn(List.of());
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(em(modPortal, "1.4.0", "1.5.0", true)));

    byte[] pdf = service.gerar(entregaId);
    assertThat(pdf).isNotEmpty();
  }

  @Test
  void gerar_entregaInexistenteLancaNotFound() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.gerar(entregaId))
        .isInstanceOf(NotFoundException.class);
  }

  private ReleaseItem item(String titulo, String descricao, VisibilidadeItem visibilidade) {
    return new ReleaseItem(release, CategoriaItem.NOVIDADE, titulo, descricao,
        visibilidade, 0, null, null, null, null);
  }

  private EntregaModulo em(ModuloProduto modulo, String from, String to, boolean selecionado) {
    return new EntregaModulo(entrega, modulo, from, to, selecionado, false, 0);
  }
}
