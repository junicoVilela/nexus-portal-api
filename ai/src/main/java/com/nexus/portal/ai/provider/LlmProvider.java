package com.nexus.portal.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contrato do provedor LLM. Implementações: fake (dev/test) e OpenAI-compatible.
 * Na extração do módulo, este contrato permanece — só muda o hosting do bean.
 */
public interface LlmProvider {

  String id();

  LlmCompletion completar(String systemPrompt, String userPrompt);

  /**
   * Solicita JSON aderente ao schema quando o provedor oferece saída estruturada.
   * Providers simples podem manter o fallback textual, com validação posterior no servidor.
   */
  default LlmCompletion completarEstruturado(
      String systemPrompt, String userPrompt, JsonNode jsonSchema) {
    return completar(systemPrompt, userPrompt);
  }
}
