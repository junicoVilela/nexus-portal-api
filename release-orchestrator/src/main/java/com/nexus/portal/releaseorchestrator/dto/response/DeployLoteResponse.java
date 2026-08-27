package com.nexus.portal.releaseorchestrator.dto.response;

import java.util.List;

public record DeployLoteResponse(
    List<DeployInstalacaoResponse> itens,
    int concluidos,
    int falhas,
    int ignorados) {}
