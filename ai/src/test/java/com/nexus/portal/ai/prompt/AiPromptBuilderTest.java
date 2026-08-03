package com.nexus.portal.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AiPromptBuilderTest {

  @Test
  void systemMencionaClassesEPlaceholder() {
    String system = AiPromptBuilder.systemGerarRascunho();
    assertThat(system).contains("screen-placeholder");
    assertThat(system).contains("doc-intro");
    assertThat(system).contains("português");
    assertThat(system).contains("FUNCIONALIDADE");
  }

  @Test
  void userIncluiBriefingETarefa() {
    String user = AiPromptBuilder.userGerarRascunho(
        "Consulta",
        "PED-001",
        "Resumo",
        "Briefing longo",
        Map.of("fluxo", "filtrar"),
        "<section/>");
    assertThat(user).contains("TAREFA=GERAR_RASCUNHO");
    assertThat(user).contains("Briefing longo");
    assertThat(user).contains("PED-001");
  }
}
