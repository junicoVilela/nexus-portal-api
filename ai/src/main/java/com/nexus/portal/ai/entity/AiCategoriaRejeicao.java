package com.nexus.portal.ai.entity;

/**
 * Por que o autor rejeitou a proposta. Categoria fechada para dar para somar no painel de
 * qualidade e achar padrões por versão de prompt; o texto livre continua em {@code motivo}.
 */
public enum AiCategoriaRejeicao {
  CONTEUDO_INCORRETO("Conteúdo incorreto ou inventado"),
  FALTOU_INFORMACAO("Faltou informação"),
  ESTRUTURA_INADEQUADA("Estrutura ou seções inadequadas"),
  MODELO_ERRADO("Modelo ou componentes errados"),
  LINGUAGEM("Tom ou linguagem"),
  OUTRO("Outro motivo");

  private final String rotulo;

  AiCategoriaRejeicao(String rotulo) {
    this.rotulo = rotulo;
  }

  public String rotulo() {
    return rotulo;
  }
}
