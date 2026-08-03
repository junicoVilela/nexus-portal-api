package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiMensagemResponse(
    UUID id,
    AiPapelMensagem papel,
    String conteudo,
    List<AiPerguntaResponse> perguntas,
    int ordem,
    OffsetDateTime createdAt) {

  public static AiMensagemResponse from(AiMensagem mensagem, List<AiPerguntaResponse> perguntas) {
    return new AiMensagemResponse(
        mensagem.getId(),
        mensagem.getPapel(),
        mensagem.getConteudo(),
        perguntas == null ? List.of() : perguntas,
        mensagem.getOrdem(),
        mensagem.getCreatedAt());
  }
}
