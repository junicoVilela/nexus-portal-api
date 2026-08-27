package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PreviewRateLimiterTest {

  private PreviewRateLimiter limitador(int limitePorMinuto) {
    PreviewRateLimiter limitador = new PreviewRateLimiter();
    ReflectionTestUtils.setField(limitador, "limitePorMinuto", limitePorMinuto);
    return limitador;
  }

  @Test
  void permiteAteOLimiteEBloqueiaDepois() {
    PreviewRateLimiter limitador = limitador(3);

    for (int i = 0; i < 3; i++) {
      limitador.registrarAcesso("tok");
    }

    assertThatThrownBy(() -> limitador.registrarAcesso("tok"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Muitos acessos");
  }

  @Test
  void contaSeparadamentePorToken() {
    PreviewRateLimiter limitador = limitador(2);
    limitador.registrarAcesso("a");
    limitador.registrarAcesso("a");

    assertThatCode(() -> limitador.registrarAcesso("b")).doesNotThrowAnyException();
  }

  @Test
  void limiteZeroDesabilitaAProtecao() {
    PreviewRateLimiter limitador = limitador(0);

    assertThatCode(() -> {
      for (int i = 0; i < 100; i++) {
        limitador.registrarAcesso("tok");
      }
    }).doesNotThrowAnyException();
  }
}
