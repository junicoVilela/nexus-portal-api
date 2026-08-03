package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PaginaTemplateDuplicarRequest(
    @NotBlank @Size(max = 120) String nome,
    UUID projetoId,
    UUID clienteId) {

  @AssertTrue(message = "Informe somente um projeto ou um cliente para a cópia.")
  public boolean isEscopoValido() {
    return (projetoId == null) != (clienteId == null);
  }
}
