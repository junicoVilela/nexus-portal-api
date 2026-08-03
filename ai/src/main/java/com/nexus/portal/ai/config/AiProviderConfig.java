package com.nexus.portal.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.provider.FakeLlmProvider;
import com.nexus.portal.ai.provider.LlmProvider;
import com.nexus.portal.ai.provider.OpenAiCompatibleProvider;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderConfig {

  @Bean
  LlmProvider llmProvider(AiProperties properties, ObjectMapper objectMapper) {
    if (properties.prontoParaGerar()) {
      return new OpenAiCompatibleProvider(properties, objectMapper);
    }
    return new FakeLlmProvider();
  }
}
