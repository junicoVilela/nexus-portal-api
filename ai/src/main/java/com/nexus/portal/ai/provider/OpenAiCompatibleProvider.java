package com.nexus.portal.ai.provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
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
    return completar(systemPrompt, userPrompt, null);
  }

  @Override
  public LlmCompletion completarEstruturado(
      String systemPrompt, String userPrompt, JsonNode jsonSchema) {
    try {
      return completar(systemPrompt, userPrompt, jsonSchema);
    } catch (IllegalStateException ex) {
      String mensagem = ex.getMessage() == null ? "" : ex.getMessage();
      if (mensagem.contains("LLM HTTP 400") || mensagem.contains("LLM HTTP 422")) {
        // Alguns modelos OpenAI-compatible não implementam response_format/json_schema.
        // O prompt ainda exige JSON e a validação/allowlist permanecem no servidor.
        return completar(systemPrompt, userPrompt, null);
      }
      throw ex;
    }
  }

  private LlmCompletion completar(
      String systemPrompt, String userPrompt, JsonNode jsonSchema) {
    try {
      Map<String, Object> payload = criarPayload(systemPrompt, userPrompt, jsonSchema);
      String body = objectMapper.writeValueAsString(payload);

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

      HttpResponse<String> response =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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

  Map<String, Object> criarPayload(
      String systemPrompt, String userPrompt, JsonNode jsonSchema) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", properties.model());
    boolean reasoningModel = isReasoningModel(properties.model());
    if (reasoningModel) {
      payload.put("reasoning_effort", properties.reasoningEffort());
      payload.put("max_completion_tokens", properties.maxTokensSaida());
    } else {
      payload.put("temperature", 0.2);
      payload.put("max_tokens", properties.maxTokensSaida());
    }
    payload.put("messages", List.of(
        Map.of("role", reasoningModel ? "developer" : "system", "content", systemPrompt),
        Map.of("role", "user", "content", userPrompt)));
    if (jsonSchema != null) {
      payload.put("response_format", Map.of(
          "type", "json_schema",
          "json_schema", Map.of(
              "name", "docflow_page_spec",
              "strict", true,
              "schema", jsonSchema)));
    }
    return payload;
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

  private static boolean isReasoningModel(String model) {
    if (model == null) {
      return false;
    }
    String normalized = model.toLowerCase(java.util.Locale.ROOT);
    return normalized.startsWith("gpt-5") || normalized.contains("/gpt-5");
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "…";
  }
}
