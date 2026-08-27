package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaginaSnippetRequest(
    @NotBlank(message = "Informe o código do trecho.")
    @Size(max = 60)
    @Pattern(regexp = "[A-Za-z0-9_-]+",
        message = "O código aceita apenas letras, números, hífen e underscore.")
    String codigo,

    @NotBlank(message = "Informe o título do trecho.")
    @Size(max = 200) String titulo,

    @Size(max = 300) String descricao,

    @NotBlank(message = "O trecho precisa ter conteúdo.") String conteudoHtml,

    Boolean ativo) {
}
