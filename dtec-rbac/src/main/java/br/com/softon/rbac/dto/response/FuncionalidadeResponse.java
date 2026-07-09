package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.Funcionalidade;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuncionalidadeResponse(
    UUID id,
    UUID dominioId,
    String dominioCodigo,
    String codigo,
    String nome,
    String descricao,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static FuncionalidadeResponse from(Funcionalidade f) {
    return new FuncionalidadeResponse(
        f.getId(),
        f.getDominio().getId(),
        f.getDominio().getCodigo(),
        f.getCodigo(),
        f.getNome(),
        f.getDescricao(),
        f.isAtivo(),
        f.getCreatedAt(),
        f.getUpdatedAt());
  }
}
