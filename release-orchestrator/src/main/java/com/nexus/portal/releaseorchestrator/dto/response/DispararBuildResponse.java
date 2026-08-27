package com.nexus.portal.releaseorchestrator.dto.response;

import java.util.List;
import java.util.UUID;

/** Resultado do disparo do job Jenkins. */
public record DispararBuildResponse(
    String origem,
    String tag,
    String versao,
    UUID releaseId,
    UUID releaseIdAfetada,
    String jenkinsJob,
    String queueUrl,
    String ultimoBuildStatus,
    String aviso,
    List<JobEnfileirado> jobs) {

  public DispararBuildResponse {
    if (jobs == null) {
      jobs = List.of();
    }
  }

  public record JobEnfileirado(
      String alvoId,
      String produtoSigla,
      String jenkinsJob,
      String tag,
      String queueUrl,
      String aviso) {}
}
