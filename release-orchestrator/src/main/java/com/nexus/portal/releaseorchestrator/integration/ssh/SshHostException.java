package com.nexus.portal.releaseorchestrator.integration.ssh;

public class SshHostException extends RuntimeException {

  public SshHostException(String message) {
    super(message);
  }

  public SshHostException(String message, Throwable cause) {
    super(message, cause);
  }
}
