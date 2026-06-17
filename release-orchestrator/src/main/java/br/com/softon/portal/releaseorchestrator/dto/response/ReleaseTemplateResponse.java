package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.ReleaseTemplate;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReleaseTemplateResponse(
        UUID id,
        String nome,
        String descricao,
        TipoRelease tipoRelease,
        UUID produtoId,
        String estrutura,
        boolean ativo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ReleaseTemplateResponse from(ReleaseTemplate t) {
        return new ReleaseTemplateResponse(
                t.getId(), t.getNome(), t.getDescricao(), t.getTipoRelease(),
                t.getProdutoId(), t.getEstrutura(), t.isAtivo(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
