package br.com.softon.rbac.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CriarUsuarioRequest(
    @NotBlank String username,
    @NotBlank String password,
    String nome,
    String email) {
}
