package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProdutoRhRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 20)  String sigla,
        @Size(max = 500)           String descricao,
        @NotBlank @Size(max = 20)  String cor,
        UUID responsavelId,
        Boolean ativo
) {}
