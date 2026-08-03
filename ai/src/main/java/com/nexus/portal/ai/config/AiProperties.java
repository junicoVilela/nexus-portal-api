package com.nexus.portal.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do módulo AI (OpenRouter por padrão — API OpenAI-compatible).
 *
 * <pre>
 * NEXUS_AI_ENABLED=true
 * NEXUS_AI_API_KEY=...                 # ou OPENROUTER_API_KEY
 * NEXUS_AI_BASE_URL=https://openrouter.ai/api/v1
 * NEXUS_AI_MODEL=openai/gpt-4o-mini
 * NEXUS_AI_HTTP_REFERER=http://localhost:4200
 * NEXUS_AI_APP_TITLE=Nexus AI
 * NEXUS_AI_MAX_GERACOES_POR_HORA=20
 * </pre>
 *
 * @see <a href="https://openrouter.ai/docs/quickstart">OpenRouter Quickstart</a>
 */
@ConfigurationProperties(prefix = "nexus.ai")
public record AiProperties(
    boolean enabled,
    String baseUrl,
    String apiKey,
    String model,
    String httpReferer,
    String appTitle,
    int timeoutSeconds,
    int maxPerguntas,
    int maxTokensSaida,
    int maxGeracoesPorHora) {

  public static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
  public static final String OPENROUTER_DEFAULT_MODEL = "openai/gpt-4o-mini";

  public AiProperties {
    if (timeoutSeconds <= 0) {
      timeoutSeconds = 90;
    }
    if (maxPerguntas <= 0) {
      maxPerguntas = 5;
    }
    if (maxTokensSaida <= 0) {
      maxTokensSaida = 8000;
    }
    if (maxGeracoesPorHora <= 0) {
      maxGeracoesPorHora = 20;
    }
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = OPENROUTER_BASE_URL;
    }
    if (model == null || model.isBlank()) {
      model = OPENROUTER_DEFAULT_MODEL;
    }
    if (httpReferer == null || httpReferer.isBlank()) {
      httpReferer = "http://localhost:4200";
    }
    if (appTitle == null || appTitle.isBlank()) {
      appTitle = "Nexus AI";
    }
  }

  public boolean temCredencial() {
    return apiKey != null && !apiKey.isBlank();
  }

  public boolean prontoParaGerar() {
    return enabled && temCredencial();
  }

  public boolean openRouter() {
    return baseUrl != null && baseUrl.toLowerCase().contains("openrouter.ai");
  }
}
