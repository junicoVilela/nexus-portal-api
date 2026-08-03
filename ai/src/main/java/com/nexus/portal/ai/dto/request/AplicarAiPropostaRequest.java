package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AplicarAiPropostaRequest(
    @NotNull ModoAplicacao modo,
    UUID moduloId,
    UUID parentId) {

  public enum ModoAplicacao {
    FORM,
    PERSISTIR
  }
}
