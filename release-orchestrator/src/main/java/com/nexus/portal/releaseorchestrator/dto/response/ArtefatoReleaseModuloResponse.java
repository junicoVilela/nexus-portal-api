package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ArtefatoReleaseModuloResponse(
    UUID id,
    UUID releaseId,
    UUID moduloProdutoId,
    String nomeArquivo,
    String sha256,
    long tamanhoBytes,
    String observacao,
    String uploadedBy,
    OffsetDateTime uploadedAt) {

  public static ArtefatoReleaseModuloResponse from(ArtefatoReleaseModulo a) {
    return new ArtefatoReleaseModuloResponse(
        a.getId(),
        a.getRelease().getId(),
        a.getModuloProduto().getId(),
        a.getNomeArquivo(),
        a.getSha256(),
        a.getTamanhoBytes(),
        a.getObservacao(),
        a.getCreatedBy(),
        a.getCreatedAt());
  }
}
