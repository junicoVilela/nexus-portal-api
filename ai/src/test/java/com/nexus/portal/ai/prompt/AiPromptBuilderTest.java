package com.nexus.portal.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AiPromptBuilderTest {

  @Test
  void systemExigePreencherModeloDaBiblioteca() {
    String system = AiPromptBuilder.systemGerarRascunho();
    assertThat(system).contains("esqueletoHtml");
    assertThat(system).containsIgnoringCase("nunca inventa layout");
    assertThat(system).contains("screen-placeholder");
    assertThat(system).contains("português");
  }

  @Test
  void userIncluiBriefingTemplateEEsqueleto() {
    String user = AiPromptBuilder.userGerarRascunho(
        "Consulta",
        "PED-001",
        "Resumo",
        "Briefing longo",
        Map.of("fluxo", "filtrar"),
        "<section class=\"doc-intro\"/>",
        "CONSULTA",
        "Consulta");
    assertThat(user).contains("TAREFA=GERAR_RASCUNHO");
    assertThat(user).contains("PREENCHER_MODELO_BIBLIOTECA");
    assertThat(user).contains("Briefing longo");
    assertThat(user).contains("PED-001");
    assertThat(user).contains("templateCodigo: CONSULTA");
    assertThat(user).contains("doc-intro");
  }
}
