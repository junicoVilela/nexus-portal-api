package com.nexus.portal.docflow.dto.response;

import java.util.List;

/** Receita editorial versionada que combina componentes sem duplicar seu HTML. */
public record PaginaBlueprintResponse(
    String id,
    String nome,
    String descricao,
    String tipoConteudo,
    int versao,
    String status,
    int minimoComponentes,
    int maximoComponentes,
    List<String> templatesCompativeis,
    List<PaginaBlueprintSecaoResponse> secoes) {
}
