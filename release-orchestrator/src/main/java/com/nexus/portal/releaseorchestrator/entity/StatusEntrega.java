package com.nexus.portal.releaseorchestrator.entity;

import java.util.Set;

/**
 * Status próprio de {@link Entrega} (a entrega executada, distinta da
 * {@link ProximaEntrega} planejada).
 *
 * Transições válidas (spec 22):
 *   RASCUNHO    → EM_GERACAO, CANCELADA
 *   EM_GERACAO  → CONCLUIDA, FALHA, CANCELADA
 *   FALHA       → EM_GERACAO (reprocessar)
 *   CONCLUIDA   → (terminal — reentrega gera entrega nova)
 *   CANCELADA   → (terminal)
 */
public enum StatusEntrega {
  RASCUNHO(Set.of("EM_GERACAO", "CANCELADA")),
  EM_GERACAO(Set.of("CONCLUIDA", "FALHA", "CANCELADA")),
  CONCLUIDA(Set.of()),
  FALHA(Set.of("EM_GERACAO")),
  CANCELADA(Set.of());

  private final Set<String> proximosPermitidos;

  StatusEntrega(Set<String> proximosPermitidos) {
    this.proximosPermitidos = proximosPermitidos;
  }

  public boolean podeTransicionarPara(StatusEntrega destino) {
    return proximosPermitidos.contains(destino.name());
  }

  public boolean terminal() {
    return proximosPermitidos.isEmpty();
  }

  public boolean editavel() {
    return this == RASCUNHO;
  }
}
