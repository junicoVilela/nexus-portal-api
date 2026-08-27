package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record AtribuirRevisorRequest(
    @NotBlank(message = "Informe o responsável pela revisão.") String revisorUsername,
    OffsetDateTime prazoRevisao) {
}
