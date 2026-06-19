package br.com.softon.portal.docflow.dto.response;

import java.util.UUID;

public record GrupoMeResponse(
    UUID id,
    String codigo,
    String nome) {
}
