package br.com.softon.portal.releaseorchestrator.integration.publish;

import java.time.OffsetDateTime;

public record PublishResult(
    String destino,
    long tamanhoBytes,
    OffsetDateTime publicadoEm) {}
