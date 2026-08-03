package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.PaginaTemplateVersao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaTemplateVersaoResponse(
    UUID id,
    int numero,
    String nome,
    String descricao,
    String conteudoHtml,
    boolean ativo,
    UUID projetoId,
    UUID clienteId,
    long paginasOriginadas,
    OffsetDateTime createdAt,
    String createdBy) {

  public static PaginaTemplateVersaoResponse from(PaginaTemplateVersao versao, long paginasOriginadas) {
    return new PaginaTemplateVersaoResponse(
        versao.getId(), versao.getNumero(), versao.getNome(), versao.getDescricao(),
        versao.getConteudoHtml(), versao.isAtivo(), versao.getProjetoId(), versao.getClienteId(),
        paginasOriginadas, versao.getCreatedAt(), versao.getCreatedBy());
  }
}
