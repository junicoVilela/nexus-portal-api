package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.service.AiPagePatch;
import java.util.Map;

/** Uma mudança proposta no ajuste de página, com o texto anterior para o diff. */
public record AiPatchOperacaoResponse(
    String id,
    String tipo,
    String unidadeId,
    String textoAntes,
    String novoTexto,
    String aposSecaoId,
    String componenteId,
    Map<String, String> textos,
    String motivo) {

  public static AiPatchOperacaoResponse from(AiPagePatch.Operacao operacao) {
    return new AiPatchOperacaoResponse(
        operacao.id(),
        operacao.tipo().name(),
        operacao.unidadeId(),
        operacao.textoAntes(),
        operacao.novoTexto(),
        operacao.aposSecaoId(),
        operacao.componenteId(),
        operacao.textos(),
        operacao.motivo());
  }
}
