package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuncionalidadeProdutoResponse(
    UUID id,
    UUID dominioProdutoId,
    String nome,
    String codigo,
    String codigoLegado,
    String codigoOperacao,
    String descricao,
    boolean critica,
    int ordem,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static FuncionalidadeProdutoResponse from(FuncionalidadeProduto f) {
    return new FuncionalidadeProdutoResponse(
        f.getId(),
        f.getDominio().getId(),
        f.getNome(),
        f.getCodigo(),
        f.getCodigoLegado(),
        f.getCodigoOperacao(),
        f.getDescricao(),
        f.isCritica(),
        f.getOrdem(),
        f.isAtivo(),
        f.getCreatedAt(),
        f.getUpdatedAt());
  }
}
