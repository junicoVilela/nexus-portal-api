package com.nexus.portal.docflow.dto.response;

import java.util.List;

/** Snapshot versionado da biblioteca composicional consumida pelo editor e pela IA. */
public record PaginaBibliotecaResponse(
    String id,
    int schemaVersion,
    List<PaginaBlocoResponse> componentes,
    List<PaginaBlueprintResponse> blueprints) {
}
