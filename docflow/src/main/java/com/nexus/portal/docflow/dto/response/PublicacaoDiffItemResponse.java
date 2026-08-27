package com.nexus.portal.docflow.dto.response;

import java.util.UUID;

public record PublicacaoDiffItemResponse(
    UUID paginaId,
    String titulo,
    String codigoTela,
    String mudanca) {
}
