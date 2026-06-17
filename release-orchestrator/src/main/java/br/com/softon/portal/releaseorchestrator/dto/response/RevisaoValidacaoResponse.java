package br.com.softon.portal.releaseorchestrator.dto.response;

import java.util.List;

public record RevisaoValidacaoResponse(
        boolean valida,
        List<String> pendencias,
        List<String> alertas,
        long totalItensCliente,
        long totalItensInternos
) {}
