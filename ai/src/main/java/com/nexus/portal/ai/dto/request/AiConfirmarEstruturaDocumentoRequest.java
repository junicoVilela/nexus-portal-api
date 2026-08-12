package com.nexus.portal.ai.dto.request;

import com.nexus.portal.ai.entity.AiDocumentoClienteModo;
import com.nexus.portal.ai.entity.AiDocumentoProjetoModo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AiConfirmarEstruturaDocumentoRequest(
    @NotNull AiDocumentoProjetoModo modoProjeto,
    @NotNull AiDocumentoClienteModo modoCliente,
    UUID projetoId,
    UUID clienteId,
    @Size(max = 150) String clienteNome,
    @Size(max = 150) String projetoNome,
    @Size(max = 1_000) String projetoDescricao,
    @NotEmpty @Size(max = 80) List<@Valid Modulo> modulos) {

  public record Modulo(
      @NotNull UUID planoId,
      @NotBlank @Size(max = 150) String nome) {}
}
