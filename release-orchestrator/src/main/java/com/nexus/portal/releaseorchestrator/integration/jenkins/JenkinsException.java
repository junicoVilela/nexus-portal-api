package com.nexus.portal.releaseorchestrator.integration.jenkins;

public class JenkinsException extends RuntimeException {

  private final int status;

  public JenkinsException(String message, int status) {
    this(message, status, null);
  }

  public JenkinsException(String message, int status, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
