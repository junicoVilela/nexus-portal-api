package com.nexus.portal.ai.provider;

/** Resultado de uma chamada LLM, com métricas opcionais de tokens. */
public record LlmCompletion(String content, Integer tokensEntrada, Integer tokensSaida) {

  public static LlmCompletion of(String content) {
    return new LlmCompletion(content, null, null);
  }

  public static LlmCompletion of(String content, Integer tokensEntrada, Integer tokensSaida) {
    return new LlmCompletion(content, tokensEntrada, tokensSaida);
  }
}
