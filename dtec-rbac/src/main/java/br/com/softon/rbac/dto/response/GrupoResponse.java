package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.Grupo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GrupoResponse(
    UUID id,
    String codigo,
    String nome,
    String descricao,
    boolean ativo,
    List<String> permissoes,
    int totalUsuarios,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {

  public static GrupoResponse from(Grupo grupo, List<String> permissaoCodigos) {
    return new GrupoResponse(
        grupo.getId(),
        grupo.getCodigo(),
        grupo.getNome(),
        grupo.getDescricao(),
        grupo.isAtivo(),
        List.copyOf(permissaoCodigos),
        grupo.getUsuarios().size(),
        grupo.getCreatedAt(),
        grupo.getUpdatedAt(),
        grupo.getCreatedBy(),
        grupo.getUpdatedBy());
  }
}
