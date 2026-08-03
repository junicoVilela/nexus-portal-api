package com.nexus.identityaccess.dto.response;

import com.nexus.identityaccess.entity.PoliticaSenha;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PoliticaSenhaResponse(
    UUID id,
    int tamanhoMinimo,
    boolean exigirMaiuscula,
    boolean exigirMinuscula,
    boolean exigirNumero,
    boolean exigirEspecial,
    Integer expiraSenhaDias,
    int quantidadeHistorico,
    int maxTentativasInvalidas,
    boolean ativo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static PoliticaSenhaResponse from(PoliticaSenha p) {
    return new PoliticaSenhaResponse(
        p.getId(), p.getTamanhoMinimo(),
        p.isExigirMaiuscula(), p.isExigirMinuscula(), p.isExigirNumero(), p.isExigirEspecial(),
        p.getExpiraSenhaDias(), p.getQuantidadeHistorico(), p.getMaxTentativasInvalidas(),
        p.isAtivo(), p.getCreatedAt(), p.getUpdatedAt());
  }
}
