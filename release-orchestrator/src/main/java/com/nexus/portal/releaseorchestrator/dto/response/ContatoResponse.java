package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.Contato;
import com.nexus.portal.releaseorchestrator.entity.PapelContato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContatoResponse(
    UUID id,
    UUID clienteId,
    String nome,
    PapelContato papel,
    String email,
    String telefone,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static ContatoResponse from(Contato c) {
    return new ContatoResponse(
        c.getId(),
        c.getCliente().getId(),
        c.getNome(),
        c.getPapel(),
        c.getEmail(),
        c.getTelefone(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
