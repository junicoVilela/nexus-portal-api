package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiProposta;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiPropostaTipo;
import java.time.OffsetDateTime;
import com.nexus.portal.ai.service.AiPagePatch;
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
    String pageSpecJson,
    boolean aptoParaRevisao,
    List<AiQualidadeItemResponse> qualidade,
    AiPropostaStatus status,
    UUID paginaId,
    OffsetDateTime createdAt,
    /** Não vazio quando a geração caiu em fallback e o conteúdo exige atenção redobrada. */
    List<String> avisosGeracao,
    String motivoRejeicao,
    /** Ajuste de página: o que a IA resumiu, as mudanças propostas e as que o autor aplicou. */
    String resumoDaMudanca,
    List<AiPatchOperacaoResponse> operacoes,
    List<String> operacoesAceitas) {

  public static AiPropostaResponse from(
      AiProposta proposta,
      boolean aptoParaRevisao,
      List<AiQualidadeItemResponse> qualidade) {
    return from(proposta, aptoParaRevisao, qualidade, null);
  }

  public static AiPropostaResponse from(
      AiProposta proposta,
      boolean aptoParaRevisao,
      List<AiQualidadeItemResponse> qualidade,
      AiPagePatch patch) {
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
        proposta.getPageSpecJson(),
        aptoParaRevisao,
        qualidade == null ? List.of() : qualidade,
        proposta.getStatus(),
        proposta.getPaginaId(),
        proposta.getCreatedAt(),
        proposta.getAvisosGeracao() == null ? List.of() : proposta.getAvisosGeracao(),
        proposta.getMotivoRejeicao(),
        patch == null ? null : patch.resumoDaMudanca(),
        patch == null ? List.of() : patch.operacoes().stream().map(AiPatchOperacaoResponse::from).toList(),
        proposta.getOperacoesAceitas() == null ? List.of() : proposta.getOperacoesAceitas());
  }
}
