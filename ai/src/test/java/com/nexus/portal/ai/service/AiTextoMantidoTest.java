package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class AiTextoMantidoTest {

  @Test
  void textoIdenticoComFormatacaoDiferenteContaComoMantido() {
    assertThat(AiTextoMantido.fracao(
        "<p>Filtre os pedidos por período.</p>",
        "<section><h2>Filtre os PEDIDOS</h2><p>por período</p></section>")).isEqualTo(1.0);
  }

  @Test
  void reescritaParcialReduzAFracao() {
    // proposta: filtre, os, pedidos, por, periodo (5); página mantém filtre, pedidos, periodo
    assertThat(AiTextoMantido.fracao("<p>Filtre os pedidos por período.</p>", "<p>Filtre pedidos no período.</p>"))
        .isCloseTo(0.6, within(0.001));
  }

  @Test
  void repeticaoNaoInflaAContagem() {
    assertThat(AiTextoMantido.fracao("<p>salvar salvar salvar</p>", "<p>salvar</p>"))
        .isCloseTo(1.0 / 3, within(0.001));
  }

  @Test
  void propostaSemTextoNaoTemMedida() {
    assertThat(AiTextoMantido.fracao("<div></div>", "<p>qualquer</p>")).isNull();
  }
}
