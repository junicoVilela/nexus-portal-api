package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.Cliente;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String nome,
    String slug,
    boolean ativo,
    boolean logoDisponivel,
    String temaCorPrimaria,
    String temaCorFundo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {

  public static ClienteResponse from(Cliente cliente) {
    return new ClienteResponse(
        cliente.getId(),
        cliente.getNome(),
        cliente.getSlug(),
        cliente.isAtivo(),
        cliente.getLogoPath() != null,
        cliente.getTemaCorPrimaria(),
        cliente.getTemaCorFundo(),
        cliente.getCreatedAt(),
        cliente.getUpdatedAt(),
        cliente.getCreatedBy(),
        cliente.getUpdatedBy());
  }
}
