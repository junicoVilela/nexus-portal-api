package com.nexus.portal.ai.service;

import org.springframework.stereotype.Service;

import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.response.AiStatusResponse;
import com.nexus.portal.ai.provider.LlmProvider;

@Service
public class AiStatusService {

  private final AiProperties properties;
  private final LlmProvider llmProvider;

  public AiStatusService(AiProperties properties, LlmProvider llmProvider) {
    this.properties = properties;
    this.llmProvider = llmProvider;
  }

  public AiStatusResponse status() {
    boolean pronto = properties.prontoParaGerar();
    String mensagem;
    if (!properties.enabled()) {
      mensagem = "Módulo AI desabilitado (nexus.ai.enabled=false).";
    } else if (!properties.temCredencial()) {
      mensagem = "Módulo AI habilitado sem API key — usando provider fake.";
    } else {
      mensagem = "Módulo AI pronto para geração.";
    }
    return new AiStatusResponse(
        properties.enabled(),
        pronto,
        llmProvider.id(),
        properties.model(),
        mensagem);
  }
}
