package br.com.softon.portal.releaseorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de storage do release-orchestrator. No MVP temos:
 * <ul>
 *   <li>{@code artefatos-dir}: onde ficam os artefatos da release (uploads).</li>
 *   <li>{@code entregas-dir}: onde ficam os ZIPs finais das entregas
 *       (geração F1.11).</li>
 * </ul>
 * Pós-MVP entram credenciais do GitHub Releases, S3/MinIO etc.
 */
@ConfigurationProperties(prefix = "release-orchestrator.storage")
public record ReleaseOrchestratorStorageProperties(String artefatosDir, String entregasDir) {
}
