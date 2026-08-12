package com.nexus.portal.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleProviderTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void lunaUsaParametrosDeReasoningEContratoEstruturado() {
    OpenAiCompatibleProvider provider = provider("openai/gpt-5.6-luna", "low");

    Map<String, Object> payload = provider.criarPayload(
        "instrucoes", "briefing", objectMapper.createObjectNode().put("type", "object"));

    assertThat(payload)
        .containsEntry("model", "openai/gpt-5.6-luna")
        .containsEntry("reasoning_effort", "low")
        .containsEntry("max_completion_tokens", 8000)
        .doesNotContainKeys("max_tokens", "temperature")
        .containsKey("response_format");
    assertThat(primeiraMensagem(payload)).containsEntry("role", "developer");
  }

  @Test
  void modeloNaoReasoningMantemContratoLegado() {
    OpenAiCompatibleProvider provider = provider("openai/gpt-4o-mini", "low");

    Map<String, Object> payload = provider.criarPayload("instrucoes", "briefing", null);

    assertThat(payload)
        .containsEntry("max_tokens", 8000)
        .containsEntry("temperature", 0.2)
        .doesNotContainKeys("max_completion_tokens", "reasoning_effort");
    assertThat(primeiraMensagem(payload)).containsEntry("role", "system");
  }

  private OpenAiCompatibleProvider provider(String model, String reasoningEffort) {
    AiProperties properties = new AiProperties(
        true,
        AiProperties.OPENROUTER_BASE_URL,
        "sk-test",
        model,
        reasoningEffort,
        "http://localhost:4200",
        "Nexus AI",
        90,
        5,
        8000,
        20);
    return new OpenAiCompatibleProvider(properties, objectMapper);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> primeiraMensagem(Map<String, Object> payload) {
    return ((List<Map<String, Object>>) payload.get("messages")).getFirst();
  }
}
