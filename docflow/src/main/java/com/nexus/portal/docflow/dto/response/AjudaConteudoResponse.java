package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.AjudaConteudo;
import com.nexus.portal.docflow.entity.TipoAjudaConteudo;
import com.nexus.portal.docflow.entity.TipoAjudaMedia;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record AjudaConteudoResponse(
    UUID id,
    String codigo,
    TipoAjudaConteudo tipo,
    String jornadaCodigo,
    String titulo,
    String resumo,
    String conteudo,
    String rotaContexto,
    String rotaAcao,
    String rotuloAcao,
    String icone,
    String seletorAlvo,
    TipoAjudaMedia mediaTipo,
    List<String> mediaUrls,
    String mediaAlt,
    int ordem,
    boolean ativo,
    OffsetDateTime updatedAt,
    String updatedBy) {

  public static AjudaConteudoResponse from(AjudaConteudo conteudo) {
    List<String> urls = conteudo.getMediaUrls() == null || conteudo.getMediaUrls().isBlank()
        ? List.of()
        : Arrays.stream(conteudo.getMediaUrls().split("\\R")).filter(url -> !url.isBlank()).toList();
    return new AjudaConteudoResponse(
        conteudo.getId(), conteudo.getCodigo(), conteudo.getTipo(), conteudo.getJornadaCodigo(),
        conteudo.getTitulo(), conteudo.getResumo(), conteudo.getConteudo(), conteudo.getRotaContexto(),
        conteudo.getRotaAcao(), conteudo.getRotuloAcao(), conteudo.getIcone(), conteudo.getSeletorAlvo(),
        conteudo.getMediaTipo(), urls, conteudo.getMediaAlt(), conteudo.getOrdem(), conteudo.isAtivo(),
        conteudo.getUpdatedAt(), conteudo.getUpdatedBy());
  }
}
