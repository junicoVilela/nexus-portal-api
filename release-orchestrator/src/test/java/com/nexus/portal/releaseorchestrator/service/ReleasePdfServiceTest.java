package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.entity.CategoriaItem;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseItem;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoPdfRelease;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.entity.VisibilidadeItem;
import com.nexus.portal.releaseorchestrator.repository.ReleaseItemRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
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
class ReleasePdfServiceTest {

  @Mock ReleaseRepository releaseRepository;
  @Mock ReleaseItemRepository releaseItemRepository;

  ReleasePdfService service;
  ProdutoRh produto;
  Release release;
  final UUID releaseId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new ReleasePdfService(releaseRepository, releaseItemRepository,
        new MarkdownPdfRenderer());
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    release = new Release(produto, "1.5.0", "Release Junho",
        TipoRelease.MINOR, ReleaseStatus.PUBLICADA,
        LocalDate.of(2026, 6, 30), null, "Inclui SSO e exportação.", null);
  }

  @Test
  void gerar_clienteRetornaPdfValido() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(releaseId))
        .thenReturn(List.of(
            item(CategoriaItem.NOVIDADE, "SSO", "Login único", VisibilidadeItem.TODOS),
            item(CategoriaItem.AJUSTE_TECNICO, "Refactor X", "Tecnico", VisibilidadeItem.TECNICO)));

    byte[] pdf = service.gerar(releaseId, TipoPdfRelease.CLIENTE);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void gerar_internoRetornaPdfValido() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(releaseId))
        .thenReturn(List.of(
            item(CategoriaItem.NOVIDADE, "SSO", "Login único", VisibilidadeItem.TODOS),
            item(CategoriaItem.AJUSTE_TECNICO, "Refactor X", "Tecnico", VisibilidadeItem.TECNICO),
            item(CategoriaItem.DOCUMENTACAO, "Runbook", "Suporte", VisibilidadeItem.SUPORTE)));

    byte[] pdf = service.gerar(releaseId, TipoPdfRelease.INTERNO);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void gerar_releaseInexistenteLancaNotFound() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.gerar(releaseId, TipoPdfRelease.CLIENTE))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void gerar_releaseSemItensProduzPdf() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(releaseItemRepository.findByReleaseIdOrderByOrdemAsc(releaseId)).thenReturn(List.of());

    byte[] pdf = service.gerar(releaseId, TipoPdfRelease.CLIENTE);
    assertThat(pdf).isNotEmpty();
  }

  @Test
  void tipoPdf_filtraVisibilidadesCorretas() {
    // CLIENTE só inclui TODOS
    assertThat(TipoPdfRelease.CLIENTE.inclui(VisibilidadeItem.TODOS)).isTrue();
    assertThat(TipoPdfRelease.CLIENTE.inclui(VisibilidadeItem.TECNICO)).isFalse();
    assertThat(TipoPdfRelease.CLIENTE.inclui(VisibilidadeItem.SUPORTE)).isFalse();

    // SUPORTE inclui TODOS + SUPORTE, exclui TECNICO
    assertThat(TipoPdfRelease.SUPORTE.inclui(VisibilidadeItem.TODOS)).isTrue();
    assertThat(TipoPdfRelease.SUPORTE.inclui(VisibilidadeItem.SUPORTE)).isTrue();
    assertThat(TipoPdfRelease.SUPORTE.inclui(VisibilidadeItem.TECNICO)).isFalse();

    // INTERNO inclui tudo
    for (VisibilidadeItem v : VisibilidadeItem.values()) {
      assertThat(TipoPdfRelease.INTERNO.inclui(v)).isTrue();
    }
  }

  private ReleaseItem item(CategoriaItem categoria, String titulo, String descricao,
      VisibilidadeItem visibilidade) {
    return new ReleaseItem(release, categoria, titulo, descricao, visibilidade, 1,
        null, null, null, null);
  }
}
