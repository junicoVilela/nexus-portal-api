package br.com.softon.portal.docflow.dto.request;

public record AtualizarUsuarioRequest(String nome, String email, boolean ativo) {
}
