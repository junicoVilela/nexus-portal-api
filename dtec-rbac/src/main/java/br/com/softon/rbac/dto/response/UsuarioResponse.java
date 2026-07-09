package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String username,
    String nome,
    String email,
    boolean ativo,
    boolean bloqueado,
    int tentativasInvalidas,
    boolean trocarSenhaProximoLogin,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {

  public static UsuarioResponse from(Usuario u) {
    return new UsuarioResponse(u.getId(), u.getUsername(), u.getNome(), u.getEmail(),
        u.isAtivo(), u.isBloqueado(), u.getTentativasInvalidas(), u.isTrocarSenhaProximoLogin(),
        u.getCreatedAt(), u.getUpdatedAt(), u.getCreatedBy(), u.getUpdatedBy());
  }
}
