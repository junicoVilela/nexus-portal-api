package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.AcaoHistorico;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseHistorico;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReleaseHistoricoResponse(
        UUID id,
        UUID releaseId,
        AcaoHistorico acao,
        String descricao,
        ReleaseStatus statusAnterior,
        ReleaseStatus statusNovo,
        String usuario,
        OffsetDateTime createdAt
) {
    public static ReleaseHistoricoResponse from(ReleaseHistorico h) {
        return new ReleaseHistoricoResponse(
                h.getId(), h.getReleaseId(), h.getAcao(), h.getDescricao(),
                h.getStatusAnterior(), h.getStatusNovo(), h.getUsuario(), h.getCreatedAt());
    }
}
