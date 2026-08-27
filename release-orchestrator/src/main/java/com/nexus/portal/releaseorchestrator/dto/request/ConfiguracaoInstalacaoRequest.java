package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ConfiguracaoInstalacaoRequest(
    TipoBanco tipoBanco,
    @Size(max = 200) String bancoHost,
    @Min(1) @Max(65535) Integer bancoPorta,
    @Size(max = 120) String bancoNome,
    @Size(max = 120) String bancoUsuario,
    @Size(max = 200) String bancoCredencialRef,
    @Size(max = 500) String urlBackend,
    @Size(max = 500) String urlFrontend,
    @Size(max = 8000) String parametros) {}
