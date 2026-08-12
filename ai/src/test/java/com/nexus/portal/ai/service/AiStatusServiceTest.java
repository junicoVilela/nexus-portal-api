package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.response.AiStatusResponse;
import com.nexus.portal.ai.provider.FakeLlmProvider;

class AiStatusServiceTest {

  @Test
  void statusDesabilitadoUsaProviderFake() {
    AiProperties props = new AiProperties(
        false, null, null, null, null, null, null, 0, 0, 0, 20);
    AiStatusService service = new AiStatusService(props, new FakeLlmProvider());

    AiStatusResponse status = service.status();

    assertThat(status.enabled()).isFalse();
    assertThat(status.prontoParaGerar()).isFalse();
    assertThat(status.provider()).isEqualTo(FakeLlmProvider.ID);
    assertThat(status.mensagem()).contains("desabilitado");
  }

  @Test
  void statusComChaveOpenRouterProntoParaGerar() {
    AiProperties props = new AiProperties(
        true,
        AiProperties.OPENROUTER_BASE_URL,
        "sk-or-test",
        "openai/gpt-5.6-luna",
        "low",
        "http://localhost:4200",
        "Nexus AI",
        30,
        3,
        1000,
        20);
    AiStatusService service = new AiStatusService(props, new FakeLlmProvider());

    AiStatusResponse status = service.status();

    assertThat(status.enabled()).isTrue();
    assertThat(status.prontoParaGerar()).isTrue();
    assertThat(status.model()).isEqualTo("openai/gpt-5.6-luna");
    assertThat(props.openRouter()).isTrue();
  }
}
