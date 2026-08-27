package com.nexus.portal.releaseorchestrator.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Opções de ref para disparar o Jenkins a partir de uma release do portal. */
public record FontesBuildResponse(
    UUID releaseId,
    String versaoRelease,
    boolean jenkinsConfigurado,
    boolean githubConfigurado,
    String jenkinsUrl,
    String jenkinsJob,
    String aviso,
    String githubErro,
    FonteBuild releaseAtual,
    FonteBuild ultimaGerada,
    List<TagBuild> tags) {

  public record FonteBuild(
      String origem,
      String tag,
      String versao,
      Integer totalAssets,
      OffsetDateTime publishedAt) {}

  public record TagBuild(
      String tag,
      String versao,
      boolean temGithubRelease,
      Integer totalAssets) {}
}
