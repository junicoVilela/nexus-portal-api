package com.nexus.portal.ai.provider;

/**
 * Contrato do provedor LLM. Implementações: fake (dev/test) e OpenAI-compatible.
 * Na extração do módulo, este contrato permanece — só muda o hosting do bean.
 */
public interface LlmProvider {

  String id();

  LlmCompletion completar(String systemPrompt, String userPrompt);
}
