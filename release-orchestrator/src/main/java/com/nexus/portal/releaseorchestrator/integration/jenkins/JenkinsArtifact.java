package com.nexus.portal.releaseorchestrator.integration.jenkins;

/** Artefato arquivado em um build Jenkins. */
public record JenkinsArtifact(String fileName, String relativePath) {

  public String pathDownload() {
    return relativePath == null || relativePath.isBlank() ? fileName : relativePath;
  }
}
