package br.com.softon.rbac.dto.response;

import java.util.UUID;

public record GrupoMeResponse(
    UUID id,
    String codigo,
    String nome) {
}
