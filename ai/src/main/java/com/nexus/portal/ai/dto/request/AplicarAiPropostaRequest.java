package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AplicarAiPropostaRequest(
    @NotNull ModoAplicacao modo,
    UUID moduloId,
    UUID parentId,
    Integer ordem,
    /** Ajuste de página: ids das operações aceitas; nulo = todas. */
    @Size(max = 25) List<String> operacoesAceitas) {

  public AplicarAiPropostaRequest(ModoAplicacao modo, UUID moduloId, UUID parentId, Integer ordem) {
    this(modo, moduloId, parentId, ordem, null);
  }

  public enum ModoAplicacao {
    FORM,
    PERSISTIR
  }
}
