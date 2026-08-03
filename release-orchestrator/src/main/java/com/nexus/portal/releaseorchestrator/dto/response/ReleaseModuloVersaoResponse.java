package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReleaseModuloVersaoResponse(
    UUID id,
    UUID releaseId,
    UUID moduloProdutoId,
    String moduloCodigo,
    String moduloNome,
    String versao,
    OffsetDateTime updatedAt) {

  public static ReleaseModuloVersaoResponse from(ReleaseModuloVersao v) {
    return new ReleaseModuloVersaoResponse(
        v.getId(),
        v.getRelease().getId(),
        v.getModuloProduto().getId(),
        v.getModuloProduto().getCodigo(),
        v.getModuloProduto().getNome(),
        v.getVersao(),
        v.getUpdatedAt());
  }
}
