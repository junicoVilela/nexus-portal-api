package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Escolha da versão a implantar (ou a gerar no Jenkins) a partir da
 * ficha da instalação. {@code tag} só é obrigatória em {@code TAG_ESPECIFICA}.
 */
public record ResolverVersaoInstalacaoRequest(
    @NotNull OrigemBuild origem,
    @Size(max = 80) String tag) {}
