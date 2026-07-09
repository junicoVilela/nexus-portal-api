package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.Permissao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PermissaoResponse(
    UUID id,
    UUID funcionalidadeId,
    String funcionalidadeCodigo,
    String dominioCodigo,
    String acao,
    String codigo,
    String descricao,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static PermissaoResponse from(Permissao p) {
    return new PermissaoResponse(
        p.getId(),
        p.getFuncionalidade().getId(),
        p.getFuncionalidade().getCodigo(),
        p.getFuncionalidade().getDominio().getCodigo(),
        p.getAcao(),
        p.getCodigo(),
        p.getDescricao(),
        p.isAtivo(),
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
