package br.com.softon.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ModuloRequest(
    @NotBlank String nome,
    String slug,
    String descricao,
    Integer ordem,
    Boolean ativo,
    @NotNull UUID projetoId) {
}
