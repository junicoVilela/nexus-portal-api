package com.nexus.portal.releaseorchestrator.entity;

import java.util.List;
import java.util.Set;

/**
 * Tipos fixos de módulo de produto (catálogo livre). Cada tipo determina
 * como o pacote é montado durante a geração da entrega (ver spec
 * `docs/release-orchestrator/10-produtos-modulos-artefatos.md`).
 */
public enum TipoModulo {
  WEB(false, true, List.of(".war", ".jar", ".zip", ".tar.gz", ".tgz", ".ear")),
  BATCH(false, true, List.of(".jar", ".zip", ".tar.gz", ".tgz")),
  BANCO(true, true, List.of(".sql", ".zip")),
  KETTLE(true, false, List.of(".ktr", ".kjb", ".zip")),
  FUNCIONALIDADES(false, true, List.of()),
  REGRAS(false, true, List.of());

  private final boolean geraDeltaDefault;
  private final boolean obrigatorioDefault;
  private final List<String> extensoesAceitasDefault;

  TipoModulo(boolean geraDeltaDefault, boolean obrigatorioDefault,
      List<String> extensoesAceitasDefault) {
    this.geraDeltaDefault = geraDeltaDefault;
    this.obrigatorioDefault = obrigatorioDefault;
    this.extensoesAceitasDefault = extensoesAceitasDefault;
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

  /**
   * Extensões default aceitas no upload de artefatos desse tipo (lowercase,
   * ordenadas da mais longa pra mais curta pra casar {@code .tar.gz} antes
   * de {@code .gz}). Spec §3.
   */
  public List<String> extensoesAceitasDefault() {
    return extensoesAceitasDefault;
  }

  /** Aceita {@code .WAR}, {@code .war}, {@code .War} — case-insensitive. */
  public boolean aceitaExtensao(String nomeArquivo) {
    if (nomeArquivo == null || extensoesAceitasDefault.isEmpty()) {
      return false;
    }
    String lower = nomeArquivo.toLowerCase();
    return extensoesAceitasDefault.stream().anyMatch(lower::endsWith);
  }

  /** Conjunto imutável das extensões default (case-insensitive matching feito em {@link #aceitaExtensao}). */
  public Set<String> extensoesAceitasDefaultSet() {
    return Set.copyOf(extensoesAceitasDefault);
  }
}
