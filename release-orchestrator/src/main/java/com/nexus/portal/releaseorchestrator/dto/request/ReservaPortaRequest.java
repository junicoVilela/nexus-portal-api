package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.PapelPorta;
import com.nexus.portal.releaseorchestrator.entity.ProtocoloPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.TipoPorta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReservaPortaRequest(
    @NotNull TipoPorta tipo,
    @NotNull PapelPorta papel,
    @NotNull @Min(1) @Max(65535) Integer porta,
    ProtocoloPorta protocolo,
    StatusReservaPorta status) {}
