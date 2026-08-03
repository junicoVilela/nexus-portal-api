package com.nexus.portal.releaseorchestrator.integration.publish;

import java.time.OffsetDateTime;

public record PublishResult(
    String destino,
    long tamanhoBytes,
    OffsetDateTime publicadoEm) {}
