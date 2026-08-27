package com.nexus.portal.docflow.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicacaoDiffResponse(
    UUID publicacaoId,
    String versao,
    UUID comparadaComId,
    String versaoComparada,
    Map<String, Long> totaisPorMudanca,
    List<PublicacaoDiffItemResponse> itens) {
}
