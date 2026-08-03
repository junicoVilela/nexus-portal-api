package com.nexus.portal.releaseorchestrator.entity;

import java.util.Set;

/**
 * Status próprio de {@link ProximaEntrega} — distinto de
 * {@link com.nexus.portal.releaseorchestrator.entity.ReleaseStatus}.
 *
 * Transições válidas (spec 17 §6):
 *   PLANEJADA   → AGENDADA, REPLANEJADA, CANCELADA
 *   AGENDADA    → REPLANEJADA, ATRASADA, CONVERTIDA, CANCELADA
 *   REPLANEJADA → AGENDADA, ATRASADA, CONVERTIDA, CANCELADA
 *   ATRASADA    → REPLANEJADA, CONVERTIDA, CANCELADA
 *   CONVERTIDA  → (terminal)
 *   CANCELADA   → (terminal)
 */
public enum StatusProximaEntrega {
  PLANEJADA(Set.of("AGENDADA", "REPLANEJADA", "CANCELADA")),
  AGENDADA(Set.of("REPLANEJADA", "ATRASADA", "CONVERTIDA", "CANCELADA")),
  REPLANEJADA(Set.of("AGENDADA", "ATRASADA", "CONVERTIDA", "CANCELADA")),
  ATRASADA(Set.of("REPLANEJADA", "CONVERTIDA", "CANCELADA")),
  CONVERTIDA(Set.of()),
  CANCELADA(Set.of());

  private final Set<String> proximosPermitidos;

  StatusProximaEntrega(Set<String> proximosPermitidos) {
    this.proximosPermitidos = proximosPermitidos;
  }

  public boolean podeTransicionarPara(StatusProximaEntrega destino) {
    return proximosPermitidos.contains(destino.name());
  }

  public boolean terminal() {
    return proximosPermitidos.isEmpty();
  }
}
