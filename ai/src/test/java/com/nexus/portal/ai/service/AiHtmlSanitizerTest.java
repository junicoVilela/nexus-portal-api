package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiHtmlSanitizerTest {

  private final AiHtmlSanitizer sanitizer = new AiHtmlSanitizer();

  @Test
  void removeScriptEMantemClassesDoc() {
    String html = """
        <section class="doc-intro"><p>Olá</p><script>alert(1)</script></section>
        <a href="javascript:void(0)">x</a>
        """;

    String clean = sanitizer.sanitizar(html);

    assertThat(clean).contains("doc-intro").contains("Olá");
    assertThat(clean).doesNotContain("<script");
    assertThat(clean).doesNotContain("javascript:");
  }
}
