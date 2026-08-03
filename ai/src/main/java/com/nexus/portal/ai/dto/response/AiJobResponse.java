package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiJobTipo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiJobResponse(
    UUID id,
    UUID sessaoId,
    AiJobTipo tipo,
    AiJobStatus status,
    String erroMensagem,
    String modelo,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt) {

  public static AiJobResponse from(AiJob job) {
    return new AiJobResponse(
        job.getId(),
        job.getSessao().getId(),
        job.getTipo(),
        job.getStatus(),
        job.getErroMensagem(),
        job.getModelo(),
        job.getStartedAt(),
        job.getFinishedAt());
  }
}
