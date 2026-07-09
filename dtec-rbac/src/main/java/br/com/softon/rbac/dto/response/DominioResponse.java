package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.Dominio;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DominioResponse(
    UUID id,
    String codigo,
    String nome,
    String descricao,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static DominioResponse from(Dominio d) {
    return new DominioResponse(d.getId(), d.getCodigo(), d.getNome(), d.getDescricao(),
        d.isAtivo(), d.getCreatedAt(), d.getUpdatedAt());
  }
}
