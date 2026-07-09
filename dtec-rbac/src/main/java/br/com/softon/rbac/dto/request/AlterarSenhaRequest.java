package br.com.softon.rbac.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaRequest(@NotBlank String novaSenha) {
}
