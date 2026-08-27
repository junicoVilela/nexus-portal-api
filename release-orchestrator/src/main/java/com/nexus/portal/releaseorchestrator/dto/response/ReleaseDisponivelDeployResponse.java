package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import java.util.UUID;

/** Release que pode ser escolhida no deploy, cruzada com o GitHub quando houver integração. */
public record ReleaseDisponivelDeployResponse(
    UUID id,
    String versao,
    String titulo,
    ReleaseStatus status,
    boolean selecionavel,
    boolean noGit,
    boolean emAndamento,
    boolean rascunhoGit,
    boolean preReleaseGit,
    String tagGit) {}
