package com.nexus.portal.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.provider.FakeLlmProvider;
import com.nexus.portal.ai.provider.LlmProvider;
import com.nexus.portal.ai.provider.OpenAiCompatibleProvider;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderConfig {

  @Bean
  LlmProvider llmProvider(AiProperties properties, ObjectMapper objectMapper, Environment environment) {
    if (properties.prontoParaGerar()) {
      return new OpenAiCompatibleProvider(properties, objectMapper);
    }
    // Em produção o fake entregaria conteúdo genérico como se fosse real: falha no boot.
    if (properties.enabled() && environment.matchesProfiles("prod")) {
      throw new IllegalStateException(
          "nexus.ai.enabled=true exige NEXUS_AI_API_KEY no perfil prod.");
    }
    return new FakeLlmProvider();
  }
}
