package com.nexus.portal.ai.entity;

/** O que o PR muda, pelos arquivos alterados. Só os dois primeiros viram proposta. */
public enum AiPrClassificacao {
  /** Arquivos de tela adicionados: provavelmente uma tela nova. */
  UI_NOVA,
  /** Só arquivos de tela modificados: tela existente mudou. */
  UI_ALTERACAO,
  SO_BACKEND,
  /** Docs, testes, build, rótulo de pular. */
  IRRELEVANTE;

  public boolean geraProposta() {
    return this == UI_NOVA || this == UI_ALTERACAO;
  }
}
