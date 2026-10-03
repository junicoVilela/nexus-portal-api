package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiPrClassificacao;
import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiPrEventoStatus;
import com.nexus.portal.ai.entity.AiPropostaStatus;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Item da fila de propostas vindas de PR. {@code pendente}: precisa de alguém — proposta
 * aguardando decisão, página aguardando rascunho ou erro para reprocessar.
 */
public record AiFilaPrItemResponse(
    UUID id,
    String repositorio,
    int numeroPr,
    String titulo,
    String url,
    String autor,
    String branchBase,
    OffsetDateTime mergedAt,
    AiPrClassificacao classificacao,
    String codigoTela,
    AiPrEventoStatus status,
    String mensagem,
    UUID sessaoId,
    AiSessaoStatus sessaoStatus,
    UUID paginaId,
    String responsavel,
    OffsetDateTime createdAt,
    AiPropostaResponse proposta,
    boolean pendente) {

  public static AiFilaPrItemResponse from(AiPrEvento evento, AiSessaoStatus sessaoStatus, AiPropostaResponse proposta) {
    boolean pendente = switch (evento.getStatus()) {
      case AGUARDANDO_RASCUNHO, ERRO, RECEBIDO -> true;
      case EM_FILA -> proposta == null || proposta.status() == AiPropostaStatus.PENDENTE;
      case IGNORADO -> false;
    };
    return new AiFilaPrItemResponse(
        evento.getId(),
        evento.getRepositorio(),
        evento.getNumeroPr(),
        evento.getTitulo(),
        evento.getUrl(),
        evento.getAutor(),
        evento.getBranchBase(),
        evento.getMergedAt(),
        evento.getClassificacao(),
        evento.getCodigoTela(),
        evento.getStatus(),
        evento.getMensagem(),
        evento.getSessaoId(),
        sessaoStatus,
        evento.getPaginaId(),
        evento.getResponsavel(),
        evento.getCreatedAt(),
        proposta,
        pendente);
  }
}
