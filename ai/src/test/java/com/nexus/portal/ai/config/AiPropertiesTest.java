package com.nexus.portal.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiPropertiesTest {

  @Test
  void defaultsApontamParaOpenRouter() {
    AiProperties props = new AiProperties(
        true, null, null, null, null, null, null, 0, 0, 0, 20);

    assertThat(props.baseUrl()).isEqualTo(AiProperties.OPENROUTER_BASE_URL);
    assertThat(props.model()).isEqualTo(AiProperties.OPENROUTER_DEFAULT_MODEL);
    assertThat(props.reasoningEffort()).isEqualTo(AiProperties.DEFAULT_REASONING_EFFORT);
    assertThat(props.openRouter()).isTrue();
    assertThat(props.appTitle()).isEqualTo("Nexus AI");
    assertThat(props.httpReferer()).isEqualTo("http://localhost:4200");
  }
}
