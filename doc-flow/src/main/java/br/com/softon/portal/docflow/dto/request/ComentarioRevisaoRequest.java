package br.com.softon.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRevisaoRequest(
    @NotBlank(message = "Escreva um comentário para a revisão.")
    @Size(max = 280, message = "O comentário deve ter no máximo 280 caracteres.")
    String comentario) {
}
