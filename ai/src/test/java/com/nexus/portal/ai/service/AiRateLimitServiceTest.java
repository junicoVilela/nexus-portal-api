package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.repository.AiJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiRateLimitServiceTest {

  private final AiJobRepository jobRepository = mock(AiJobRepository.class);
  private AiRateLimitService service;

  @BeforeEach
  void setUp() {
    service = new AiRateLimitService(props(2), jobRepository);
  }

  @Test
  void bloqueiaQuandoUsuarioAtingiuOLimiteNaUltimaHora() {
    when(jobRepository.countBySessaoCreatedByAndCreatedAtAfter(eq("alice"), any())).thenReturn(2L);

    assertThatThrownBy(() -> service.exigirGeracaoPermitida(() -> "alice"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void contaPorUsuario() {
    when(jobRepository.countBySessaoCreatedByAndCreatedAtAfter(eq("a"), any())).thenReturn(2L);
    when(jobRepository.countBySessaoCreatedByAndCreatedAtAfter(eq("b"), any())).thenReturn(1L);

    assertThatCode(() -> service.exigirGeracaoPermitida(() -> "b")).doesNotThrowAnyException();
  }

  private static AiProperties props(int maxPorHora) {
    return new AiProperties(true, null, "k", "m", null, null, null, 30, 5, 1000, maxPorHora);
  }
}
