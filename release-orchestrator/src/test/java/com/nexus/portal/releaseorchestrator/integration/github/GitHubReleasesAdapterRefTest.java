package com.nexus.portal.releaseorchestrator.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitHubReleasesAdapterRefTest {

  @Test
  void nomeCurto_stripsHeadsAndTags() {
    assertThat(GitHubReleasesAdapter.GitHubRefDto.nomeCurto("refs/heads/release/v5.4.1"))
        .isEqualTo("release/v5.4.1");
    assertThat(GitHubReleasesAdapter.GitHubRefDto.nomeCurto("refs/tags/v5.3.58.1"))
        .isEqualTo("v5.3.58.1");
  }
}
