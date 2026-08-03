package com.nexus.portal.ai.dto.response;

import java.util.UUID;

public record AiAplicacaoResponse(
    String modo,
    UUID propostaId,
    UUID paginaId,
    String titulo,
    String slug,
    String codigoTela,
    String resumo,
    String conteudoHtml,
    UUID templateOrigemId,
    Integer templateOrigemVersao,
    UUID moduloId) {
}
