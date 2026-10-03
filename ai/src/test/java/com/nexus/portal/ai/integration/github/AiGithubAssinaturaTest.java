package com.nexus.portal.ai.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AiGithubAssinaturaTest {

  private static final byte[] CORPO = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);

  @Test
  void exemploDaDocumentacaoDoGithub() {
    // https://docs.github.com/webhooks/using-webhooks/validating-webhook-deliveries#testing-the-webhook-payload-validation
    assertThat(AiGithubAssinatura.assinar("It's a Secret to Everybody", "Hello, World!".getBytes(StandardCharsets.UTF_8)))
        .isEqualTo("sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17");
  }

  @Test
  void aceitaSoAAssinaturaDoMesmoSecretECorpo() {
    String assinatura = AiGithubAssinatura.assinar("segredo", CORPO);

    assertThat(AiGithubAssinatura.valida("segredo", CORPO, assinatura)).isTrue();
    assertThat(AiGithubAssinatura.valida("outro", CORPO, assinatura)).isFalse();
    assertThat(AiGithubAssinatura.valida("segredo", "{}".getBytes(StandardCharsets.UTF_8), assinatura)).isFalse();
    assertThat(AiGithubAssinatura.valida("segredo", CORPO, null)).isFalse();
    assertThat(AiGithubAssinatura.valida("segredo", CORPO, "sha1=abc")).isFalse();
    assertThat(AiGithubAssinatura.valida("", CORPO, assinatura)).isFalse();
  }
}
