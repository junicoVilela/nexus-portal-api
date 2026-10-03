package com.nexus.portal.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.ai.prompt.AiPromptCatalogo.Prompt;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Valida os arquivos de {@code resources/prompts}: quem edita um prompt descobre o erro no build. */
class AiPromptCatalogoTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "gerar-page-spec.system",
      "gerar-page-spec.user",
      "analise-documento.system",
      "analise-documento.user",
      "analise-documento-amplo.system",
      "analise-documento-amplo.user"})
  void todoPromptTemVersaoETexto(String nome) {
    Prompt prompt = AiPromptCatalogo.carregar(nome);
    assertThat(prompt.versao()).isPositive();
    assertThat(prompt.texto()).isNotBlank().doesNotStartWith("---");
    assertThat(prompt.id()).isEqualTo(nome + "@" + prompt.versao());
  }

  @Test
  void systemPromptsNaoTemVariaveis() {
    for (String nome : Set.of("gerar-page-spec", "analise-documento", "analise-documento-amplo")) {
      assertThat(AiPromptCatalogo.carregar(nome + ".system").variaveis()).as(nome).isEmpty();
    }
  }

  @Test
  void variaveisDoPromptDePaginaBatemComOCodigo() {
    assertThat(AiPromptCatalogo.carregar("gerar-page-spec.user").variaveis()).containsExactlyInAnyOrder(
        "templateCodigo", "templateNome", "titulo", "codigoTela", "resumo", "briefing", "contexto",
        "blueprint", "catalogo", "ajustes");
  }

  @Test
  void renderizarFalhaQuandoFaltaOuSobraVariavel() {
    Prompt prompt = new Prompt("teste", 1, "Olá {{nome}}");
    assertThatThrownBy(() -> prompt.renderizar(Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("[nome]");
    assertThatThrownBy(() -> prompt.renderizar(Map.of("nome", "a", "extra", "b")))
        .isInstanceOf(IllegalStateException.class);
    assertThat(prompt.renderizar(Map.of("nome", "Ana"))).isEqualTo("Olá Ana");
  }

  @Test
  void promptInexistenteFalhaComMensagemClara() {
    assertThatThrownBy(() -> AiPromptCatalogo.carregar("nao-existe"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("prompts/nao-existe.md");
  }
}
