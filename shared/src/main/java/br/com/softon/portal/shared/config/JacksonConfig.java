package br.com.softon.portal.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
