package com.nexus.portal.releaseorchestrator.integration.github;

/**
 * Arquivo alterado entre dois commits, conforme retornado por
 * GET /repos/{owner}/{repo}/compare/{base}...{head}.
 *
 * @param status added | modified | renamed | removed | copied | changed | unchanged
 */
public record GitHubFileChange(
    String filename,
    String previousFilename,
    String status,
    int additions,
    int deletions,
    int changes,
    /** SHA do blob na head — usado em downloads. */
    String sha) {

  public boolean foiAdicionadoOuModificado() {
    return "added".equals(status) || "modified".equals(status)
        || "renamed".equals(status) || "changed".equals(status) || "copied".equals(status);
  }
}
