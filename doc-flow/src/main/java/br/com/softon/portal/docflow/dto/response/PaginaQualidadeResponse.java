package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.service.PaginaQualidadeService.ResultadoQualidade;
import java.util.List;

public record PaginaQualidadeResponse(
    boolean aptoParaRevisao,
    long concluidos,
    int total,
    List<ItemQualidadeResponse> itens) {

  public static PaginaQualidadeResponse from(ResultadoQualidade resultado) {
    List<ItemQualidadeResponse> itens = resultado.itens().stream()
        .map(item -> new ItemQualidadeResponse(item.codigo(), item.titulo(), item.descricao(),
            item.ok(), item.severidade().name()))
        .toList();
    return new PaginaQualidadeResponse(resultado.aptoParaRevisao(),
        itens.stream().filter(ItemQualidadeResponse::ok).count(), itens.size(), itens);
  }

  public record ItemQualidadeResponse(
      String codigo,
      String titulo,
      String descricao,
      boolean ok,
      String severidade) {
  }
}
