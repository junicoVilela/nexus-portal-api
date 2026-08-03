package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.Publicacao;
import java.util.List;

public record ReprocessamentoPublicacoesResponse(
    int solicitadas,
    int reprocessadas,
    int ignoradas,
    List<PublicacaoResponse> publicacoes) {

  public static ReprocessamentoPublicacoesResponse from(int solicitadas, List<Publicacao> publicacoes) {
    return new ReprocessamentoPublicacoesResponse(
        solicitadas,
        publicacoes.size(),
        Math.max(0, solicitadas - publicacoes.size()),
        publicacoes.stream().map(PublicacaoResponse::from).toList());
  }
}
