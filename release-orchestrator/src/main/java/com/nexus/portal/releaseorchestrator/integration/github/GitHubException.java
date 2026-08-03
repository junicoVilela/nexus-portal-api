package com.nexus.portal.releaseorchestrator.integration.github;

/** Falha ao interagir com a API do GitHub. */
public class GitHubException extends RuntimeException {

  private final int status;

  public GitHubException(String message, int status, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
