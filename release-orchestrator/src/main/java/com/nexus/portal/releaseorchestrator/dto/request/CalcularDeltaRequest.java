package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Body opcional do POST /entregas/{id}/delta/calcular. Permite ao operador
 * sobrescrever o FROM_TAG por módulo (spec 20) — útil quando o cliente pulou
 * versões ou recebeu hotfix manual.
 *
 * <p>Quando {@code modulos} for null ou vazio, o cálculo usa o default
 * (FROM = ClienteProdutoModulo.versaoAtual, TO = release.versao).
 */
public record CalcularDeltaRequest(
    @Valid List<OverrideModulo> modulos) {

  public record OverrideModulo(
      @NotNull UUID moduloProdutoId,
      @Size(max = 80) String fromTag,
      /** Justificativa obrigatória quando fromTag for editado manualmente. */
      @Size(max = 500) String justificativa) {}

  public static CalcularDeltaRequest vazio() {
    return new CalcularDeltaRequest(List.of());
  }
}
