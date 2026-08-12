package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiJob;
import com.nexus.portal.ai.entity.AiJobEtapa;
import com.nexus.portal.ai.entity.AiJobStatus;
import com.nexus.portal.ai.entity.AiJobTipo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiJobResponse(
    UUID id,
    UUID sessaoId,
    AiJobTipo tipo,
    AiJobStatus status,
    AiJobEtapa etapa,
    int progresso,
    int tentativa,
    String erroMensagem,
    UUID diagnosticoId,
    String modelo,
    Integer tokensEntrada,
    Integer tokensSaida,
    long duracaoMs,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    OffsetDateTime heartbeatAt,
    OffsetDateTime cancelRequestedAt) {

  public static AiJobResponse from(AiJob job) {
    return new AiJobResponse(
        job.getId(),
        job.getSessao().getId(),
        job.getTipo(),
        job.getStatus(),
        job.getEtapa(),
        job.getProgresso(),
        job.getTentativa(),
        job.getErroMensagem(),
        job.getDiagnosticoId(),
        job.getModelo(),
        job.getTokensEntrada(),
        job.getTokensSaida(),
        job.latenciaMs(),
        job.getStartedAt(),
        job.getFinishedAt(),
        job.getHeartbeatAt(),
        job.getCancelRequestedAt());
  }
}
