package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DominioProdutoRequest(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Size(max = 80) @Pattern(regexp = "[a-z0-9_-]+",
        message = "código aceita apenas a-z, 0-9, _ e -") String codigo,
    @Size(max = 80) String codigoLegado,
    @Size(max = 1000) String descricao,
    Integer ordem) {}
