package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String nome,
    String razaoSocial,
    String cnpj,
    String sigla,
    boolean ativo,
    UUID responsavelComercialId,
    AmbientePadrao ambientePadrao,
    TipoBanco tipoBanco,
    String codificacao,
    String fusoHorario,
    String observacoes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static ClienteResponse from(Cliente c) {
    return new ClienteResponse(
        c.getId(),
        c.getNome(),
        c.getRazaoSocial(),
        c.getCnpj(),
        c.getSigla(),
        c.isAtivo(),
        c.getResponsavelComercialId(),
        c.getAmbientePadrao(),
        c.getTipoBanco(),
        c.getCodificacao(),
        c.getFusoHorario(),
        c.getObservacoes(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
