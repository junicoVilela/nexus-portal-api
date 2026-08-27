package com.nexus.portal.releaseorchestrator.entity;

/**
 * CRIAR/ATUALIZAR = {@code deploy(releaseId, instalacaoId)}.
 * INICIAR/PARAR = ciclo de vida da instalação já materializada (start.sh/stop.sh ou docker).
 */
public enum OperacaoDeploy {
  CRIAR,
  ATUALIZAR,
  INICIAR,
  PARAR;

  public boolean cicloVida() {
    return this == INICIAR || this == PARAR;
  }
}
