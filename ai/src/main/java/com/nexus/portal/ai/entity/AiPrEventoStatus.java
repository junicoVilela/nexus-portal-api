package com.nexus.portal.ai.entity;

/**
 * Processamento do PR. Depois de {@code EM_FILA} a decisão (aceitar/rejeitar) fica na proposta
 * da sessão, não aqui.
 */
public enum AiPrEventoStatus {
  RECEBIDO,
  /** Classificado como sem impacto na documentação. */
  IGNORADO,
  /** A página do PR está aprovada/publicada: o ajuste só é gerado depois de voltar a rascunho. */
  AGUARDANDO_RASCUNHO,
  /** Sessão aberta e geração enfileirada: a proposta aparece na fila. */
  EM_FILA,
  /** Falha ao buscar o PR ou ao gerar; dá para reprocessar pela fila. */
  ERRO
}
