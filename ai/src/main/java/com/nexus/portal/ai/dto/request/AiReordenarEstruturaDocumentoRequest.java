package com.nexus.portal.ai.dto.request;

import com.nexus.portal.ai.entity.AiPaginaPlanoOrigem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
      @NotBlank @Size(max = 150) String nome,
      @NotNull @Size(max = 80) List<@Valid Pagina> paginas) {}

  public record Pagina(
      @NotNull UUID planoId,
      @NotBlank @Size(max = 180) String titulo,
      @NotBlank @Size(max = 200_000) String conteudo,
      @NotNull AiPaginaPlanoOrigem origem,
      boolean ajustadaManualmente) {}
}
