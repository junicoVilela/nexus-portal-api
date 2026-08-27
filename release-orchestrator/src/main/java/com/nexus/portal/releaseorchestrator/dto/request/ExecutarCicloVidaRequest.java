package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;

public record ExecutarCicloVidaRequest(ModoDeploy modo) {

  public ModoDeploy modoOuPadrao() {
    return modo == null ? ModoDeploy.REAL : modo;
  }
}
