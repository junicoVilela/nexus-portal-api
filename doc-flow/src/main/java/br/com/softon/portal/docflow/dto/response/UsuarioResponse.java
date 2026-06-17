package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String username,
    String nome,
    String email,
    String roles,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {

  public static UsuarioResponse from(Usuario u) {
    return new UsuarioResponse(u.getId(), u.getUsername(), u.getNome(), u.getEmail(), u.getRoles(),
        u.isAtivo(), u.getCreatedAt(), u.getUpdatedAt(), u.getCreatedBy(), u.getUpdatedBy());
  }
}
