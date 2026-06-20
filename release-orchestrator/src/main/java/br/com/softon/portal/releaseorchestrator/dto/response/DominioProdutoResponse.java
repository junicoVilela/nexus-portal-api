package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DominioProdutoResponse(
    UUID id,
    UUID produtoId,
    String nome,
    String codigo,
    String codigoLegado,
    String descricao,
    int ordem,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static DominioProdutoResponse from(DominioProduto d) {
    return new DominioProdutoResponse(
        d.getId(),
        d.getProduto().getId(),
        d.getNome(),
        d.getCodigo(),
        d.getCodigoLegado(),
        d.getDescricao(),
        d.getOrdem(),
        d.isAtivo(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
