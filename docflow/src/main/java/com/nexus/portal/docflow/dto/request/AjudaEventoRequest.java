package com.nexus.portal.docflow.dto.request;

import com.nexus.portal.docflow.entity.TipoAjudaEvento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AjudaEventoRequest(
    @NotNull TipoAjudaEvento tipo,
    @Size(max = 80) String conteudoCodigo,
    @Size(max = 240) String termo,
    @Size(max = 240) String rota,
    @Size(max = 80) String sessaoId,
    Integer resultadoQuantidade) {
}
