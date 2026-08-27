package com.nexus.portal.releaseorchestrator.dto.response;

import java.util.UUID;

/** Release do portal resolvida para {@code deploy(releaseId, instalacaoId)}. */
public record ResolverVersaoInstalacaoResponse(
    UUID releaseId,
    String tag,
    String versao,
    String origem,
    boolean criada,
    String aviso) {}
