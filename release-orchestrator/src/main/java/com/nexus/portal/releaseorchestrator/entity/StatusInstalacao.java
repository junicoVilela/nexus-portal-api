package com.nexus.portal.releaseorchestrator.entity;

/**
 * Situação operacional da instalação (RF-006 / RF-007).
 *
 * <p>{@code INEXISTENTE} permite cadastrar o alvo antes do serviço existir no host.
 */
public enum StatusInstalacao {
  INEXISTENTE,
  ATIVA,
  INATIVA
}
