package com.nexus.portal.releaseorchestrator.entity;

/**
 * {@code DRY_RUN} registra o contrato sem falar com o host.
 * {@code REAL} fica para os adaptadores (Docker/SSH/WinRM).
 */
public enum ModoDeploy {
  DRY_RUN,
  REAL
}
