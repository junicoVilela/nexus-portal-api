package br.com.softon.portal.releaseorchestrator.entity;

/**
 * Tipos fixos de módulo de produto (catálogo livre). Cada tipo determina
 * como o pacote é montado durante a geração da entrega (ver spec
 * `docs/release-orchestrator/10-produtos-modulos-artefatos.md`).
 */
public enum TipoModulo {
  WEB(false, true),
  BATCH(false, true),
  BANCO(true, true),
  KETTLE(true, false),
  FUNCIONALIDADES(false, true),
  REGRAS(false, true);

  private final boolean geraDeltaDefault;
  private final boolean obrigatorioDefault;

  TipoModulo(boolean geraDeltaDefault, boolean obrigatorioDefault) {
    this.geraDeltaDefault = geraDeltaDefault;
    this.obrigatorioDefault = obrigatorioDefault;
  }

  public boolean geraDeltaDefault() {
    return geraDeltaDefault;
  }

  public boolean obrigatorioDefault() {
    return obrigatorioDefault;
  }

  public boolean aceitaUploadDeArtefato() {
    return this != FUNCIONALIDADES && this != REGRAS;
  }
}
