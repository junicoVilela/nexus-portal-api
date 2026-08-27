package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ExecutarDeployRequest(
    @NotNull UUID releaseId,
    @NotNull UUID instalacaoId,
    UUID entregaId,
    Boolean forcar,
    ModoDeploy modo) {

  public ModoDeploy modoOuPadrao() {
    return modo == null ? ModoDeploy.DRY_RUN : modo;
  }
}
