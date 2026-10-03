package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.Size;

/** Corpo opcional de {@code POST /sessoes/{id}/gerar}: ajuste pedido pelo autor antes de regenerar. */
public record GerarAiPropostaRequest(@Size(max = 2_000) String instrucao) {
}
