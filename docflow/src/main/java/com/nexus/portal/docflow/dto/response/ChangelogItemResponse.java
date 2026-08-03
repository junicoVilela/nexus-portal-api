package com.nexus.portal.docflow.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChangelogItemResponse(
    UUID id,
    UUID paginaId,
    String paginaTitulo,
    String tipoMudanca,
    OffsetDateTime createdAt) {
}
