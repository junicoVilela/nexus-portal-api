package com.nexus.portal.docflow.dto.response;

import java.util.List;

/** Seção semântica de um blueprint, vinculada a um componente confiável do catálogo. */
public record PaginaBlueprintSecaoResponse(
    String slot,
    String componenteId,
    String necessidade,
    boolean repetivel,
    int maximoInstancias,
    List<String> alternativas) {
}
