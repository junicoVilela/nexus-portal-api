package com.nexus.portal.releaseorchestrator.entity;

/**
 * Situação da reserva no inventário (RF-005). Ocupam o host:
 * {@code RESERVADA}, {@code EM_USO} e {@code BLOQUEADA}.
 */
public enum StatusReservaPorta {
  DISPONIVEL,
  RESERVADA,
  EM_USO,
  LIBERADA,
  BLOQUEADA
}
