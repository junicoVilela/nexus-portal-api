package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Dispara o job Jenkins do produto para gerar WAR/JAR.
 *
 * <p>{@code tag} é obrigatória só quando {@code origem} = {@code TAG_ESPECIFICA}.
 * Aceita {@code v1.5.0} ou {@code 1.5.0} (o portal prefixa {@code v}).
 *
 * <p>{@code alvoIds} (ficha da instalação): módulos ({@code modulo:uuid}) e/ou
 * outros produtos ({@code produto:uuid}). Vazio/nulo = só os alvos do produto
 * da instalação.
 */
public record DispararBuildRequest(
    @NotNull OrigemBuild origem,
    @Size(max = 80) String tag,
    List<@Size(max = 80) String> alvoIds) {

  public DispararBuildRequest(OrigemBuild origem, String tag) {
    this(origem, tag, null);
  }
}
