package br.com.softon.portal.releaseorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de storage do release-orchestrator. No MVP só temos
 * `artefatos-dir` (filesystem local); no pós-MVP entram credenciais
 * do GitHub Releases, S3/MinIO etc.
 */
@ConfigurationProperties(prefix = "release-orchestrator.storage")
public record ReleaseOrchestratorStorageProperties(String artefatosDir) {
}
