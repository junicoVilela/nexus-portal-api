package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiImportacaoStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Importação em andamento do usuário, para "Continuar de onde parou" no assistente. */
public record AiImportacaoResumoResponse(
    UUID id,
    String nomeArquivo,
    String projetoNome,
    AiImportacaoStatus status,
    boolean estruturaConfirmada,
    int paginasTotal,
    int paginasRevisadas,
    OffsetDateTime atualizadoEm) {
}
