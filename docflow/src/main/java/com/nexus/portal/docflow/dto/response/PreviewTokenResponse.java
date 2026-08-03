package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.PreviewToken;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PreviewTokenResponse(
    UUID id,
    UUID clienteId,
    String token,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt,
    String createdBy) {

  public static PreviewTokenResponse from(PreviewToken pt) {
    return new PreviewTokenResponse(
        pt.getId(),
        pt.getClienteId(),
        pt.getToken(),
        pt.getExpiresAt(),
        pt.getCreatedAt(),
        pt.getCreatedBy());
  }
}
