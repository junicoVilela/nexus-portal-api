package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiMensagemResponse(
    UUID id,
    AiPapelMensagem papel,
    String conteudo,
    List<AiPerguntaResponse> perguntas,
    /** O que a triagem entendeu do briefing (titulo, codigoTela, publico…); só em mensagens do assistente. */
    Map<String, String> contexto,
    int ordem,
    OffsetDateTime createdAt) {

  public static AiMensagemResponse from(
      AiMensagem mensagem,
      List<AiPerguntaResponse> perguntas,
      Map<String, String> contexto) {
    return new AiMensagemResponse(
        mensagem.getId(),
        mensagem.getPapel(),
        mensagem.getConteudo(),
        perguntas == null ? List.of() : perguntas,
        contexto == null ? Map.of() : contexto,
        mensagem.getOrdem(),
        mensagem.getCreatedAt());
  }
}
