package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ManualSinonimosTest {

  @Test
  void normalizaSemAcentoSemCaixaEPontuacaoViraEspaco() {
    assertThat(ManualSinonimos.normalizar("  Emissão da NF-e! ")).isEqualTo("emissao da nf e");
    assertThat(ManualSinonimos.normalizar(null)).isEmpty();
  }

  @Test
  void trocaOTermoPorCadaOutroDoGrupo() {
    var alternativas = ManualSinonimos.alternativas("Como emitir NF?",
        List.of(List.of("nota fiscal", "nf", "nf e")));

    assertThat(alternativas).containsExactly("Como emitir NF?", "como emitir nota fiscal", "como emitir nf e");
  }

  @Test
  void soCasaPalavraInteira() {
    assertThat(ManualSinonimos.alternativas("informações do pedido", List.of(List.of("nf", "nota fiscal"))))
        .containsExactly("informações do pedido");
  }

  @Test
  void frasesDoGrupoTambemSaoTrocadas() {
    assertThat(ManualSinonimos.alternativas("cancelar nota fiscal", List.of(List.of("nota fiscal", "nf"))))
        .contains("cancelar nf");
  }

  @Test
  void limitaAsLeituras() {
    var grupo = List.of("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");
    assertThat(ManualSinonimos.alternativas("a1", List.of(grupo))).hasSize(ManualSinonimos.MAX_ALTERNATIVAS);
  }
}
