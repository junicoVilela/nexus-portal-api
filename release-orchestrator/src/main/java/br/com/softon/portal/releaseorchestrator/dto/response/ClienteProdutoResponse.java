package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteProdutoResponse(
    UUID id,
    UUID clienteId,
    UUID produtoId,
    String produtoSigla,
    String produtoNome,
    AmbientePadrao ambiente,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static ClienteProdutoResponse from(ClienteProduto cp) {
    return new ClienteProdutoResponse(
        cp.getId(),
        cp.getCliente().getId(),
        cp.getProduto().getId(),
        cp.getProduto().getSigla(),
        cp.getProduto().getNome(),
        cp.getAmbiente(),
        cp.isAtivo(),
        cp.getCreatedAt(),
        cp.getUpdatedAt());
  }
}
