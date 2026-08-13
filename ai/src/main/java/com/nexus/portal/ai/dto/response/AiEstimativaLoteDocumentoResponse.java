package com.nexus.portal.ai.dto.response;

public record AiEstimativaLoteDocumentoResponse(
    int paginas,
    int caracteresEntrada,
    int tokensEntradaEstimados,
    int tokensSaidaEstimados,
    String modelo,
    String observacao) {}
