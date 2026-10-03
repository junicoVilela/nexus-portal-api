package com.nexus.portal.ai.service;

import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.repository.AiJobRepository;
import java.security.Principal;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Limita gerações ({@code POST .../gerar}) por usuário numa janela de 1 hora, contando os jobs
 * em {@code tb_ai_job}: o limite sobrevive a reinícios e vale entre instâncias.
 */
@Service
public class AiRateLimitService {

  private final AiProperties properties;
  private final AiJobRepository jobRepository;

  public AiRateLimitService(AiProperties properties, AiJobRepository jobRepository) {
    this.properties = properties;
    this.jobRepository = jobRepository;
  }

  public void exigirGeracaoPermitida(Principal principal) {
    int max = properties.maxGeracoesPorHora();
    if (max <= 0) {
      return;
    }
    long recentes = jobRepository.countBySessaoCreatedByAndCreatedAtAfter(
        AiSessaoService.usuario(principal), OffsetDateTime.now().minusHours(1));
    if (recentes >= max) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Limite de gerações AI atingido (" + max + "/hora). Aguarde e tente novamente.");
    }
  }
}
