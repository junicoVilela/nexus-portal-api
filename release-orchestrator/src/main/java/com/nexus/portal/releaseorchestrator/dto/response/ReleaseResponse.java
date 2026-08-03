package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReleaseResponse(
        UUID id,
        UUID produtoId,
        String produtoNome,
        String produtoSigla,
        String produtoCor,
        String versao,
        String titulo,
        TipoRelease tipo,
        ReleaseStatus status,
        LocalDate dataPrevista,
        LocalDate dataPublicacao,
        String publicadoPor,
        UUID responsavelId,
        String resumo,
        String observacoes,
        Long totalItens,
        /* S11 P2 — último build reportado pelo Jenkins via webhook */
        String ultimoBuildStatus,
        Integer ultimoBuildNumero,
        String ultimoBuildUrl,
        OffsetDateTime ultimoBuildAt,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ReleaseResponse from(Release r) {
        return from(r, null);
    }

    public static ReleaseResponse from(Release r, Long totalItens) {
        return new ReleaseResponse(
                r.getId(),
                r.getProduto().getId(),
                r.getProduto().getNome(),
                r.getProduto().getSigla(),
                r.getProduto().getCor(),
                r.getVersao(),
                r.getTitulo(),
                r.getTipo(),
                r.getStatus(),
                r.getDataPrevista(),
                r.getDataPublicacao(),
                r.getPublicadoPor(),
                r.getResponsavelId(),
                r.getResumo(),
                r.getObservacoes(),
                totalItens,
                r.getUltimoBuildStatus(),
                r.getUltimoBuildNumero(),
                r.getUltimoBuildUrl(),
                r.getUltimoBuildAt(),
                r.getCreatedBy(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
