package com.nexus.identityaccess.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaRequest(@NotBlank String novaSenha) {
}
