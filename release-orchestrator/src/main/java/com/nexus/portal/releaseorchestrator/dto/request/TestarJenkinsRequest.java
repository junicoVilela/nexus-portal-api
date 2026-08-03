package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Body opcional do POST /produtos/{id}/testar-jenkins. Permite testar
 * credenciais antes de salvar o produto.
 */
public record TestarJenkinsRequest(
    @Size(max = 300) String jenkinsUrl,
    @Size(max = 200) String jenkinsJob,
    @Size(max = 120) String jenkinsUser,
    @Size(max = 500) String jenkinsToken) {

  public boolean temOverride() {
    return (jenkinsUrl != null && !jenkinsUrl.isBlank())
        || (jenkinsJob != null && !jenkinsJob.isBlank());
  }
}
