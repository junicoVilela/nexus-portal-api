package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiSessaoResponse(
    UUID id,
    AiObjetivo objetivo,
    AiSessaoStatus status,
    UUID projetoId,
    UUID moduloId,
    UUID clienteId,
    UUID paginaId,
    UUID templateId,
    String briefing,
    List<AiMensagemResponse> mensagens,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static AiSessaoResponse from(AiSessao sessao, List<AiMensagemResponse> mensagens) {
    return new AiSessaoResponse(
        sessao.getId(),
        sessao.getObjetivo(),
        sessao.getStatus(),
        sessao.getProjetoId(),
        sessao.getModuloId(),
        sessao.getClienteId(),
        sessao.getPaginaId(),
        sessao.getTemplateId(),
        sessao.getBriefing(),
        mensagens,
        sessao.getCreatedAt(),
        sessao.getUpdatedAt());
  }
}
