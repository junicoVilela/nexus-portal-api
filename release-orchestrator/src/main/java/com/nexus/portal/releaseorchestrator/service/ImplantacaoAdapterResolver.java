package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImplantacaoAdapterResolver {

  private final DryRunImplantacaoAdapter dryRun;
  private final DockerPullImplantacaoAdapter dockerPull;
  private final LinuxManualImplantacaoAdapter linuxManual;

  public ImplantacaoAdapter resolver(ModoDeploy modo, TipoImplantacao tipo) {
    if (modo == null || modo == ModoDeploy.DRY_RUN) {
      return dryRun;
    }
    if (tipo == TipoImplantacao.DOCKER_PULL) {
      return dockerPull;
    }
    if (tipo == TipoImplantacao.LINUX_MANUAL) {
      return linuxManual;
    }
    throw new BusinessException(
        "Modo REAL só está disponível para Docker pull e Linux manual. Os demais tipos continuam em dry-run.");
  }
}
