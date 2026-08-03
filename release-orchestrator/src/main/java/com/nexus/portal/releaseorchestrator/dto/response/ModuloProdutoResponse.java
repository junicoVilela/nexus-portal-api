package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ModuloProdutoResponse(
    UUID id,
    UUID produtoId,
    String codigo,
    String nome,
    TipoModulo tipo,
    boolean geraDelta,
    boolean obrigatorio,
    int ordem,
    boolean ativo,
    String configEspecifica,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static ModuloProdutoResponse from(ModuloProduto m) {
    return new ModuloProdutoResponse(
        m.getId(),
        m.getProduto().getId(),
        m.getCodigo(),
        m.getNome(),
        m.getTipo(),
        m.isGeraDelta(),
        m.isObrigatorio(),
        m.getOrdem(),
        m.isAtivo(),
        m.getConfigEspecifica(),
        m.getCreatedAt(),
        m.getUpdatedAt());
  }
}
