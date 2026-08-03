package com.nexus.portal.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugUtilsTest {

  @ParameterizedTest
  @CsvSource({
      "Olá Mundo, ola-mundo",
      "Tela de Login, tela-de-login",
      "Múltiplos   Espaços, multiplos-espacos",
      "Ação & Reação, acao-reacao",
      "já-é-slug, ja-e-slug",
      "MAIÚSCULAS, maiusculas",
      "com.ponto/barra, com-ponto-barra",
  })
  void normalize_deveConverterParaSlugValido(String entrada, String esperado) {
    assertThat(SlugUtils.normalize(entrada)).isEqualTo(esperado);
  }

  @Test
  void normalize_comEntradaNula_deveRetornarNull() {
    assertThat(SlugUtils.normalize(null)).isNull();
  }

  @Test
  void normalize_comEntradaVazia_deveRetornarNull() {
    assertThat(SlugUtils.normalize("")).isNull();
    assertThat(SlugUtils.normalize("   ")).isNull();
  }

  @Test
  void normalize_comApenasCaracteresEspeciais_deveRetornarNull() {
    assertThat(SlugUtils.normalize("---")).isNull();
    assertThat(SlugUtils.normalize("!!!")).isNull();
  }

  @Test
  void normalize_comTracoNoInicio_deveRemover() {
    assertThat(SlugUtils.normalize("-inicio")).isEqualTo("inicio");
  }

  @Test
  void normalize_comTracoNoFim_deveRemover() {
    assertThat(SlugUtils.normalize("fim-")).isEqualTo("fim");
  }
}
