package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiPropostaResponse(
    UUID id,
    UUID sessaoId,
    UUID jobId,
    AiPropostaTipo tipo,
    String titulo,
    String slug,
    String codigoTela,
    String resumo,
    String conteudoHtml,
    UUID templateId,
    Integer templateVersao,
    boolean aptoParaRevisao,
    List<AiQualidadeItemResponse> qualidade,
    AiPropostaStatus status,
    UUID paginaId,
    OffsetDateTime createdAt) {

  public static AiPropostaResponse from(
      AiProposta proposta,
      boolean aptoParaRevisao,
      List<AiQualidadeItemResponse> qualidade) {
    return new AiPropostaResponse(
        proposta.getId(),
        proposta.getSessao().getId(),
        proposta.getJob().getId(),
        proposta.getTipo(),
        proposta.getTitulo(),
        proposta.getSlug(),
        proposta.getCodigoTela(),
        proposta.getResumo(),
        proposta.getConteudoHtml(),
        proposta.getTemplateId(),
        proposta.getTemplateVersao(),
        aptoParaRevisao,
        qualidade == null ? List.of() : qualidade,
        proposta.getStatus(),
        proposta.getPaginaId(),
        proposta.getCreatedAt());
  }
}
