package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ConfiguracaoInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import java.util.UUID;

public record ConfiguracaoInstalacaoResponse(
    UUID id,
    TipoBanco tipoBanco,
    String bancoHost,
    Integer bancoPorta,
    String bancoNome,
    String bancoUsuario,
    String bancoCredencialRef,
    String urlBackend,
    String urlFrontend,
    String parametros) {

  public static ConfiguracaoInstalacaoResponse from(ConfiguracaoInstalacao c) {
    if (c == null) {
      return null;
    }
    return new ConfiguracaoInstalacaoResponse(
        c.getId(),
        c.getTipoBanco(),
        c.getBancoHost(),
        c.getBancoPorta(),
        c.getBancoNome(),
        c.getBancoUsuario(),
        c.getBancoCredencialRef(),
        c.getUrlBackend(),
        c.getUrlFrontend(),
        c.getParametros());
  }
}
