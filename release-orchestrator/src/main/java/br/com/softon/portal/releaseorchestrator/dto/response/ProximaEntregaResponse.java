package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.PrioridadeEntrega;
import br.com.softon.portal.releaseorchestrator.entity.ProximaEntrega;
import br.com.softon.portal.releaseorchestrator.entity.StatusProximaEntrega;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProximaEntregaResponse(
    UUID id,
    UUID clienteId,
    String clienteSigla,
    String clienteNome,
    UUID produtoId,
    String produtoSigla,
    String produtoNome,
    UUID releaseId,
    String releaseVersao,
    LocalDate dataPrevista,
    AmbientePadrao ambiente,
    PrioridadeEntrega prioridade,
    StatusProximaEntrega status,
    UUID responsavelId,
    String observacoes,
    UUID entregaConvertidaId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static ProximaEntregaResponse from(ProximaEntrega p) {
    var release = p.getRelease();
    return new ProximaEntregaResponse(
        p.getId(),
        p.getCliente().getId(),
        p.getCliente().getSigla(),
        p.getCliente().getNome(),
        p.getProduto().getId(),
        p.getProduto().getSigla(),
        p.getProduto().getNome(),
        release != null ? release.getId() : null,
        release != null ? release.getVersao() : null,
        p.getDataPrevista(),
        p.getAmbiente(),
        p.getPrioridade(),
        p.getStatus(),
        p.getResponsavelId(),
        p.getObservacoes(),
        p.getEntregaConvertidaId(),
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
