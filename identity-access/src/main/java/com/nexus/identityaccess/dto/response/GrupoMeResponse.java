package com.nexus.identityaccess.dto.response;

import java.util.UUID;

public record GrupoMeResponse(
    UUID id,
    String codigo,
    String nome) {
}
