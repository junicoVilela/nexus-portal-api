package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.CategoriaItem;
import com.nexus.portal.releaseorchestrator.entity.ReleaseItem;
import com.nexus.portal.releaseorchestrator.entity.VisibilidadeItem;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReleaseItemResponse(
        UUID id,
        UUID releaseId,
        CategoriaItem categoria,
        String titulo,
        String descricao,
        VisibilidadeItem visibilidade,
        int ordem,
        String ticket,
        String commit,
        String pullRequest,
        UUID responsavelId,
        OffsetDateTime createdAt
) {
    public static ReleaseItemResponse from(ReleaseItem i) {
        return new ReleaseItemResponse(
                i.getId(),
                i.getRelease().getId(),
                i.getCategoria(),
                i.getTitulo(),
                i.getDescricao(),
                i.getVisibilidade(),
                i.getOrdem(),
                i.getTicket(),
                i.getCommit(),
                i.getPullRequest(),
                i.getResponsavelId(),
                i.getCreatedAt());
    }
}
