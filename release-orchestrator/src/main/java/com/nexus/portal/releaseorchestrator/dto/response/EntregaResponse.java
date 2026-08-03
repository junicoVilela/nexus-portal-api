package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.entity.StatusPublicacao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EntregaResponse(
    UUID id,
    UUID clienteId,
    String clienteSigla,
    String clienteNome,
    UUID produtoId,
    String produtoSigla,
    String produtoNome,
    UUID releaseId,
    String releaseVersao,
    UUID proximaEntregaId,
    UUID entregaOriginalId,
    AmbientePadrao ambiente,
    StatusEntrega status,
    OffsetDateTime dataInicioGeracao,
    OffsetDateTime dataConclusao,
    UUID responsavelId,
    String arquivoPacoteCaminho,
    String arquivoPacoteSha256,
    Long tamanhoBytes,
    String observacoes,
    String falhaMotivo,
    /* --- Publicação remota (F3 P2) --- */
    StatusPublicacao statusPublicacao,
    int tentativasPublicacao,
    OffsetDateTime proximaTentativaEm,
    String ultimaFalhaPublicacao,
    OffsetDateTime dataPublicacao,
    String destinoPublicacao,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static EntregaResponse from(Entrega e) {
    return new EntregaResponse(
        e.getId(),
        e.getCliente().getId(),
        e.getCliente().getSigla(),
        e.getCliente().getNome(),
        e.getProduto().getId(),
        e.getProduto().getSigla(),
        e.getProduto().getNome(),
        e.getRelease().getId(),
        e.getRelease().getVersao(),
        e.getProximaEntregaId(),
        e.getEntregaOriginalId(),
        e.getAmbiente(),
        e.getStatus(),
        e.getDataInicioGeracao(),
        e.getDataConclusao(),
        e.getResponsavelId(),
        e.getArquivoPacoteCaminho(),
        e.getArquivoPacoteSha256(),
        e.getTamanhoBytes(),
        e.getObservacoes(),
        e.getFalhaMotivo(),
        e.getStatusPublicacao(),
        e.getTentativasPublicacao(),
        e.getProximaTentativaEm(),
        e.getUltimaFalhaPublicacao(),
        e.getDataPublicacao(),
        e.getDestinoPublicacao(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
