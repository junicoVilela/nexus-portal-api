package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.PaginaTemplate;
import java.util.UUID;

public record PaginaTemplateResponse(
    UUID id,
    String codigo,
    String nome,
    String descricao,
    String conteudoHtml,
    int ordem,
    boolean ativo,
    boolean personalizado,
    int versaoAtual,
    long paginasOriginadas,
    UUID projetoId,
    String projetoNome,
    UUID clienteId,
    String clienteNome) {

  public static PaginaTemplateResponse from(PaginaTemplate template, long paginasOriginadas) {
    return new PaginaTemplateResponse(
        template.getId(),
        template.getCodigo(),
        template.getNome(),
        template.getDescricao(),
        template.getConteudoHtml(),
        template.getOrdem(),
        template.isAtivo(),
        template.isPersonalizado(),
        template.getVersaoAtual(),
        paginasOriginadas,
        template.getProjeto() == null ? null : template.getProjeto().getId(),
        template.getProjeto() == null ? null : template.getProjeto().getNome(),
        template.getCliente() == null ? null : template.getCliente().getId(),
        template.getCliente() == null ? null : template.getCliente().getNome());
  }

  public static PaginaTemplateResponse from(PaginaTemplate template) {
    return from(template, 0);
  }
}
