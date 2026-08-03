package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.ai.config.AiProperties;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiRateLimitServiceTest {

  private AiRateLimitService service;

  @BeforeEach
  void setUp() {
    AiProperties props = new AiProperties(true, null, "k", "m", null, null, 30, 5, 1000, 2);
    service = new AiRateLimitService(props);
  }

  @Test
  void bloqueiaAposLimite() {
    Principal user = () -> "alice";
    service.exigirGeracaoPermitida(user);
    service.exigirGeracaoPermitida(user);

    assertThatThrownBy(() -> service.exigirGeracaoPermitida(user))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void usuariosIsolados() {
    service.exigirGeracaoPermitida(() -> "a");
    service.exigirGeracaoPermitida(() -> "a");
    assertThatCode(() -> service.exigirGeracaoPermitida(() -> "b")).doesNotThrowAnyException();
  }
}
