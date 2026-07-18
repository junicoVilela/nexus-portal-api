package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.StatusPagina;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaginaResponse(
    UUID id,
    long version,
    String titulo,
    String slug,
    String codigoTela,
    String resumo,
    String conteudoHtml,
    StatusPagina status,
    int ordem,
    boolean ativo,
    UUID moduloId,
    String moduloNome,
    UUID projetoId,
    String projetoNome,
    UUID parentId,
    String parentTitulo,
    UUID templateOrigemId,
    Integer templateOrigemVersao,
    OffsetDateTime publishedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {
  public static PaginaResponse from(Pagina pagina) {
    return new PaginaResponse(
        pagina.getId(),
        pagina.getVersion(),
        pagina.getTitulo(),
        pagina.getSlug(),
        pagina.getCodigoTela(),
        pagina.getResumo(),
        pagina.getConteudoHtml(),
        pagina.getStatus(),
        pagina.getOrdem(),
        pagina.isAtivo(),
        pagina.getModulo().getId(),
        pagina.getModulo().getNome(),
        pagina.getModulo().getProjeto().getId(),
        pagina.getModulo().getProjeto().getNome(),
        pagina.getParent() == null ? null : pagina.getParent().getId(),
        pagina.getParent() == null ? null : pagina.getParent().getTitulo(),
        pagina.getTemplateOrigemId(),
        pagina.getTemplateOrigemVersao(),
        pagina.getPublishedAt(),
        pagina.getCreatedAt(),
        pagina.getUpdatedAt(),
        pagina.getCreatedBy(),
        pagina.getUpdatedBy());
  }
}
