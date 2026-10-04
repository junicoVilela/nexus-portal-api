package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.ai.entity.AiFilaOrigem;
import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiPrEventoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge.PaginaAjuste;
import com.nexus.portal.ai.repository.AiPrEventoRepository;
import com.nexus.portal.shared.events.ReleasePublicadaEvento;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiReleaseFilaListenerTest {

  private final DocFlowAiBridge bridge = mock(DocFlowAiBridge.class);
  private final AiPrEventoRepository repositorio = mock(AiPrEventoRepository.class);
  private final AiReleaseFilaListener listener = new AiReleaseFilaListener(bridge, repositorio);
  private final UUID releaseId = UUID.randomUUID();

  private ReleasePublicadaEvento release() {
    return new ReleasePublicadaEvento(releaseId, "Portal", "1.5.0", "Pedidos",
        "- Filtro por status na PED-001\n- Ajuste em PED-001 e CLI-999", "/release-orchestrator/releases/" + releaseId);
  }

  @Test
  void telaCitadaComPaginaVaiParaAFilaEFicaDesatualizada() {
    UUID paginaId = UUID.randomUUID();
    when(bridge.buscarPaginaPorCodigoTela(anyString())).thenReturn(Optional.empty());
    when(bridge.buscarPaginaPorCodigoTela("PED-001")).thenReturn(Optional.of(new PaginaAjuste(paginaId,
        UUID.randomUUID(), UUID.randomUUID(), "Pedidos", "pedidos", "PED-001", "r", "<p/>", 3, "PUBLICADO")));

    listener.aoPublicar(release());

    verify(bridge).marcarPaginaDesatualizada(paginaId, "Portal 1.5.0");
    ArgumentCaptor<AiPrEvento> salvo = ArgumentCaptor.forClass(AiPrEvento.class);
    verify(repositorio).save(salvo.capture());
    assertThat(salvo.getValue().getOrigem()).isEqualTo(AiFilaOrigem.RELEASE);
    assertThat(salvo.getValue().getStatus()).isEqualTo(AiPrEventoStatus.PARA_REVISAR);
    assertThat(salvo.getValue().getCodigoTela()).isEqualTo("PED-001");
    assertThat(salvo.getValue().getCorpo()).contains("Filtro por status");
  }

  @Test
  void releaseRepublicadaNaoDuplicaItens() {
    when(bridge.buscarPaginaPorCodigoTela(anyString())).thenReturn(Optional.empty());
    when(bridge.buscarPaginaPorCodigoTela("PED-001")).thenReturn(Optional.of(new PaginaAjuste(UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), "Pedidos", "pedidos", "PED-001", "r", "<p/>", 3, "RASCUNHO")));
    when(repositorio.existsByReleaseIdAndCodigoTela(releaseId, "PED-001")).thenReturn(true);

    listener.aoPublicar(release());

    verify(repositorio, never()).save(any());
  }
}
