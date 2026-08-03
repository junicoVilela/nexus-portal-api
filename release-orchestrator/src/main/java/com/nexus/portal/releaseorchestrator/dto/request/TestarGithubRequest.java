package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body opcional do POST /produtos/{id}/testar-github. Permite sobrescrever
 * temporariamente repo/token sem ter salvado o produto ainda.
 */
public record TestarGithubRequest(
    @Size(max = 200) String repositorioGithub,
    @Size(max = 500) String githubToken) {

  /** True se foi informado override no body. */
  public boolean temOverride() {
    return repositorioGithub != null && !repositorioGithub.isBlank();
  }
}
