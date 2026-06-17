package br.com.softon.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaRequest(@NotBlank String novaSenha) {
}
