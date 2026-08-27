package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import java.util.UUID;

public record InstalacaoAlvoResponse(
    UUID id,
    String codigo,
    String nome,
    String hostCodigo,
    TipoImplantacao tipoImplantacao,
    StatusInstalacao status,
    HealthInstalacao health,
    String versaoAtual) {

  public static InstalacaoAlvoResponse from(InstalacaoCliente i) {
    return new InstalacaoAlvoResponse(
        i.getId(),
        i.getCodigo(),
        i.getNome(),
        i.getHost().getCodigo(),
        i.getTipoImplantacao(),
        i.getStatus(),
        i.getHealth(),
        i.getVersaoAtual());
  }
}
