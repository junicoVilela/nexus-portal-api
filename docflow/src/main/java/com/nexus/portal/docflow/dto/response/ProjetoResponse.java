package com.nexus.portal.docflow.dto.response;

import com.nexus.portal.docflow.entity.Projeto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjetoResponse(
    UUID id,
    String nome,
    String slug,
    String descricao,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {
  public static ProjetoResponse from(Projeto projeto) {
    return new ProjetoResponse(projeto.getId(), projeto.getNome(), projeto.getSlug(), projeto.getDescricao(),
        projeto.isAtivo(), projeto.getCreatedAt(), projeto.getUpdatedAt(), projeto.getCreatedBy(),
        projeto.getUpdatedBy());
  }
}
