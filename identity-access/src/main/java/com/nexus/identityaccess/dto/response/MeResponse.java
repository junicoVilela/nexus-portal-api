package com.nexus.identityaccess.dto.response;

import com.nexus.identityaccess.dto.response.GrupoMeResponse;
import java.util.List;
import java.util.UUID;

public record MeResponse(
    UUID id,
    String username,
    String nome,
    String email,
    List<GrupoMeResponse> grupos,
    List<String> permissoes) {
}
