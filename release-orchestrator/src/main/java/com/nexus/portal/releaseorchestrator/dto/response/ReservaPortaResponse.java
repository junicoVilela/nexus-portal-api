package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.PapelPorta;
import com.nexus.portal.releaseorchestrator.entity.ProtocoloPorta;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.TipoPorta;
import java.util.UUID;

public record ReservaPortaResponse(
    UUID id,
    TipoPorta tipo,
    PapelPorta papel,
    int porta,
    ProtocoloPorta protocolo,
    StatusReservaPorta status) {

  public static ReservaPortaResponse from(ReservaPorta p) {
    return new ReservaPortaResponse(
        p.getId(),
        p.getTipo(),
        p.getPapel(),
        p.getPorta(),
        p.getProtocolo(),
        p.getStatus());
  }
}
