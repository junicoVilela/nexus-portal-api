package com.nexus.portal.releaseorchestrator.integration.github;

import java.time.OffsetDateTime;
import java.util.List;

/** Resposta resumida do GET /repos/{owner}/{repo}/releases. */
public record GitHubRelease(
    String tagName,
    String name,
    boolean draft,
    boolean prerelease,
    OffsetDateTime publishedAt,
    List<Asset> assets
) {

  public record Asset(
      Long id,
      String name,
      Long size,
      String contentType,
      String browserDownloadUrl
  ) {}
}
