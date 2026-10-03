package com.nexus.portal.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FakeLlmProviderTest {

  private final FakeLlmProvider provider = new FakeLlmProvider();

  @Test
  void completarPadraoRetornaPerguntas() {
    var out = provider.completar("system", "user");
    assertThat(provider.id()).isEqualTo(FakeLlmProvider.ID);
    assertThat(out.content()).contains("PERGUNTAS").contains("codigoTela");
  }
}
