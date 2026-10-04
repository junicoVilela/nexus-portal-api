package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiFilaOrigem;
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
    AiFilaOrigem origem,
    String repositorio,
    Integer numeroPr,
    String titulo,
    /** Descrição do PR ou resumo e itens da release. */
    String corpo,
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
    boolean pendente,
    /** INT-601: capturas desta tela no DocFlow — podem ter ficado velhas com a mudança. */
    long capturasDaTela) {

  public static AiFilaPrItemResponse from(AiPrEvento evento, AiSessaoStatus sessaoStatus, AiPropostaResponse proposta,
      long capturasDaTela) {
    boolean pendente = switch (evento.getStatus()) {
      case AGUARDANDO_RASCUNHO, ERRO, RECEBIDO, PARA_REVISAR -> true;
      case EM_FILA -> proposta == null || proposta.status() == AiPropostaStatus.PENDENTE;
      case IGNORADO -> false;
    };
    return new AiFilaPrItemResponse(
        evento.getId(),
        evento.getOrigem(),
        evento.getRepositorio(),
        evento.getNumeroPr(),
        evento.getTitulo(),
        evento.getCorpo(),
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
        pendente,
        capturasDaTela);
  }
}
