package com.nexus.portal.ai.provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;

/**
 * Cliente HTTP para APIs compatíveis com OpenAI Chat Completions ({@code /chat/completions}).
 *
 * <p>Usado com OpenRouter ({@code https://openrouter.ai/api/v1}) enviando
 * {@code HTTP-Referer} e {@code X-Title} / {@code X-OpenRouter-Title}.
 */
public final class OpenAiCompatibleProvider implements LlmProvider {

  public static final String ID_OPENROUTER = "openrouter";
  public static final String ID_OPENAI_COMPATIBLE = "openai-compatible";

  private final AiProperties properties;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OpenAiCompatibleProvider(AiProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
        .build();
  }

  @Override
  public String id() {
    return properties.openRouter() ? ID_OPENROUTER : ID_OPENAI_COMPATIBLE;
  }

  @Override
  public LlmCompletion completar(String systemPrompt, String userPrompt) {
    try {
      String body = objectMapper.writeValueAsString(Map.of(
          "model", properties.model(),
          "temperature", 0.2,
          "max_tokens", properties.maxTokensSaida(),
          "messages", List.of(
              Map.of("role", "system", "content", systemPrompt),
              Map.of("role", "user", "content", userPrompt))));

      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(trimSlash(properties.baseUrl()) + "/chat/completions"))
          .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
          .header("Authorization", "Bearer " + properties.apiKey())
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body));

      if (properties.openRouter()) {
        builder.header("HTTP-Referer", properties.httpReferer());
        builder.header("X-Title", properties.appTitle());
        builder.header("X-OpenRouter-Title", properties.appTitle());
      }

      HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "LLM HTTP " + response.statusCode() + ": " + truncate(response.body(), 400));
      }

      JsonNode root = objectMapper.readTree(response.body());
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (content.isMissingNode() || content.asText().isBlank()) {
        throw new IllegalStateException("Resposta LLM sem content");
      }

      Integer promptTokens = intOrNull(root.path("usage").path("prompt_tokens"));
      Integer completionTokens = intOrNull(root.path("usage").path("completion_tokens"));
      return LlmCompletion.of(content.asText(), promptTokens, completionTokens);
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao chamar LLM: " + ex.getMessage(), ex);
    }
  }

  private static Integer intOrNull(JsonNode node) {
    if (node == null || node.isMissingNode() || !node.canConvertToInt()) {
      return null;
    }
    return node.asInt();
  }

  private static String trimSlash(String url) {
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
