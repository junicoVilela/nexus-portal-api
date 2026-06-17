package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProdutoRhResponse(
        UUID id,
        String nome,
        String sigla,
        String descricao,
        String cor,
        UUID responsavelId,
        boolean ativo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProdutoRhResponse from(ProdutoRh p) {
        return new ProdutoRhResponse(
                p.getId(), p.getNome(), p.getSigla(), p.getDescricao(),
                p.getCor(), p.getResponsavelId(), p.isAtivo(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
