package com.nexus.portal.docflow.dto.response;

import java.util.UUID;

public record PublicacaoPaginaSnapshotItem(
    UUID id,
    UUID parentId,
    String titulo,
    String codigoTela,
    String slug,
    int ordem,
    int nivel) {
}
