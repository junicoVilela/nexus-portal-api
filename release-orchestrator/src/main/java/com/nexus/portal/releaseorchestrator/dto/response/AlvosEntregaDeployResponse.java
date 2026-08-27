package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import java.util.List;
import java.util.UUID;

public record AlvosEntregaDeployResponse(UUID releaseId, UUID entregaId, List<Alvo> alvos) {

  public record Alvo(UUID instalacaoId, TipoImplantacao tipoImplantacao) {}
}
