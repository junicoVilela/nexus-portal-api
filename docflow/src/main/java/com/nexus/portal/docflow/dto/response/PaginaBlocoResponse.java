package com.nexus.portal.docflow.dto.response;

import java.util.List;

public record PaginaBlocoResponse(
    String id,
    String nome,
    String descricao,
    String categoria,
    String visual,
    String html,
    String parametrizacao,
    int versao,
    List<PaginaBlocoSlotResponse> slots) {
}
