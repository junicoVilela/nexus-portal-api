package br.com.softon.portal.releaseorchestrator.integration.jenkins;

/** Resumo do último build retornado pelo Jenkins. */
public record JenkinsBuildInfo(
    int number,
    /** SUCCESS | FAILURE | UNSTABLE | ABORTED | null (building) */
    String result,
    boolean building,
    long timestamp,
    /** ms */
    long duration,
    String url) {}
