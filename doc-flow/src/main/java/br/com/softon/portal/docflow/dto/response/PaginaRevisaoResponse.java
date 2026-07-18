package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.PaginaRevisao;
import br.com.softon.portal.docflow.entity.StatusPagina;
import br.com.softon.portal.docflow.entity.TipoRevisaoPagina;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaRevisaoResponse(
    UUID id,
    int numero,
    String titulo,
    StatusPagina status,
    TipoRevisaoPagina tipo,
    String descricao,
    String resumo,
    String conteudoHtml,
    OffsetDateTime createdAt,
    String createdBy) {
  public static PaginaRevisaoResponse from(PaginaRevisao revisao) {
    return new PaginaRevisaoResponse(revisao.getId(), revisao.getNumero(), revisao.getTitulo(),
        revisao.getStatus(), revisao.getTipo(), revisao.getDescricao(), revisao.getResumo(),
        revisao.getConteudoHtml(), revisao.getCreatedAt(), revisao.getCreatedBy());
  }
}
