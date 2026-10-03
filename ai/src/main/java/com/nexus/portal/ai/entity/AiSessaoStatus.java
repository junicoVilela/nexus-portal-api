package com.nexus.portal.ai.entity;

/**
 * Ciclo da sessão do assistente:
 *
 * <pre>
 * ABERTA → AGUARDANDO_USUARIO ⇄ PRONTA_PARA_GERAR → GERANDO → PRONTA ─→ APLICADA
 *                                                     ↓   ↑      │
 *                                                    ERRO ┘      └─→ (regenerar) GERANDO
 * qualquer estado não terminal → CANCELADA
 * </pre>
 *
 * As regras de "o que pode acontecer agora" ficam aqui, não espalhadas pelos services.
 */
public enum AiSessaoStatus {
  ABERTA,
  AGUARDANDO_USUARIO,
  PRONTA_PARA_GERAR,
  GERANDO,
  PRONTA,
  APLICADA,
  CANCELADA,
  ERRO;

  /** Sem volta: nada mais muda a sessão. */
  public boolean terminal() {
    return this == APLICADA || this == CANCELADA;
  }

  /**
   * Pode enfileirar geração. {@code GERANDO} entra porque um job sem heartbeat é expirado e a
   * sessão precisa aceitar a nova tentativa; o job ativo de verdade é tratado antes, no service.
   */
  public boolean permiteGerar() {
    return this == PRONTA_PARA_GERAR || this == PRONTA || this == ERRO || this == GERANDO;
  }

  /** Aceita respostas ou correções do autor (refaz a triagem). */
  public boolean aceitaMensagem() {
    return !terminal() && this != GERANDO;
  }
}
