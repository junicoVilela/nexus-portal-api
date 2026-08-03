package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ClienteFuncionalidadeProduto;
import com.nexus.portal.releaseorchestrator.entity.OrigemFuncionalidade;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteFuncionalidadeResponse(
    UUID id,
    UUID clienteId,
    UUID funcionalidadeProdutoId,
    String funcionalidadeCodigo,
    String funcionalidadeNome,
    UUID dominioProdutoId,
    String dominioCodigo,
    String dominioNome,
    UUID produtoId,
    boolean habilitada,
    OrigemFuncionalidade origem,
    OffsetDateTime updatedAt) {

  public static ClienteFuncionalidadeResponse from(ClienteFuncionalidadeProduto cf) {
    var func = cf.getFuncionalidade();
    var dominio = func.getDominio();
    return new ClienteFuncionalidadeResponse(
        cf.getId(),
        cf.getCliente().getId(),
        func.getId(),
        func.getCodigo(),
        func.getNome(),
        dominio.getId(),
        dominio.getCodigo(),
        dominio.getNome(),
        dominio.getProduto().getId(),
        cf.isHabilitada(),
        cf.getOrigem(),
        cf.getUpdatedAt());
  }
}
