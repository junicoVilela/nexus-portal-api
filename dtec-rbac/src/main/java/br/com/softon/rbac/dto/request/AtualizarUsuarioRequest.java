package br.com.softon.rbac.dto.request;

public record AtualizarUsuarioRequest(String nome, String email, boolean ativo) {
}
