package br.com.softon.portal.docflow.dto.response;

import java.util.List;
import java.util.UUID;

public record MeResponse(
    UUID id,
    String username,
    String nome,
    String email,
    List<String> roles,
    List<GrupoMeResponse> grupos,
    List<String> permissoes) {
}
