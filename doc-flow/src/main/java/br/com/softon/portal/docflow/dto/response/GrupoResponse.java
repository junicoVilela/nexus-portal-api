package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.Grupo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GrupoResponse(
    UUID id,
    String nome,
    String descricao,
    boolean ativo,
    List<String> permissoes,
    int totalUsuarios,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {

  public static GrupoResponse from(Grupo grupo) {
    return new GrupoResponse(
        grupo.getId(),
        grupo.getNome(),
        grupo.getDescricao(),
        grupo.isAtivo(),
        List.copyOf(grupo.getPermissoes()),
        grupo.getUsuarios().size(),
        grupo.getCreatedAt(),
        grupo.getUpdatedAt(),
        grupo.getCreatedBy(),
        grupo.getUpdatedBy());
  }
}
