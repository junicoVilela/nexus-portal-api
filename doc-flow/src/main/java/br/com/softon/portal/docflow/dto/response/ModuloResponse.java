package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.Modulo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ModuloResponse(
    UUID id,
    String nome,
    String slug,
    String descricao,
    int ordem,
    boolean ativo,
    UUID projetoId,
    String projetoNome,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {
  public static ModuloResponse from(Modulo modulo) {
    return new ModuloResponse(modulo.getId(), modulo.getNome(), modulo.getSlug(), modulo.getDescricao(),
        modulo.getOrdem(), modulo.isAtivo(), modulo.getProjeto().getId(), modulo.getProjeto().getNome(),
        modulo.getCreatedAt(), modulo.getUpdatedAt(), modulo.getCreatedBy(), modulo.getUpdatedBy());
  }
}
