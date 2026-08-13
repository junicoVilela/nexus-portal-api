package com.nexus.portal.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AiReordenarEstruturaDocumentoRequest(
    @NotNull @PositiveOrZero Long version,
    @NotEmpty @Size(max = 30) List<@Valid Modulo> modulos) {

  public record Modulo(
      @NotNull UUID planoId,
      @NotEmpty @Size(max = 80) List<@NotNull UUID> paginas) {}
}
