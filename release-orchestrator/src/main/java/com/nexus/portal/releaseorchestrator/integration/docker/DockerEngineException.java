package com.nexus.portal.releaseorchestrator.integration.docker;

public class DockerEngineException extends RuntimeException {

  public DockerEngineException(String message) {
    super(message);
  }

  public DockerEngineException(String message, Throwable cause) {
    super(message, cause);
  }
}
