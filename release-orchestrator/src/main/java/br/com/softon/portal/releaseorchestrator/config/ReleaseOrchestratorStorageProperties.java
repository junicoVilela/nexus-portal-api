package br.com.softon.portal.releaseorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de storage do release-orchestrator.
 *
 * <ul>
 *   <li>{@code artefatos-dir}: artefatos da release (uploads + cache GitHub).</li>
 *   <li>{@code entregas-dir}: ZIPs finais das entregas (geração F1.11).</li>
 *   <li>{@code retencao-dias}: idade máxima (em dias) que um pacote ZIP
 *       fica em disco depois de gerado. Default 90. Zero ou negativo
 *       desliga a retenção (mantém tudo).</li>
 * </ul>
 *
 * <p>Storage local — decisão de deploy registrada no memo
 * {@code storage-disco-local-servidor.md}. Sem NFS/S3 no MVP.
 */
@ConfigurationProperties(prefix = "release-orchestrator.storage")
public record ReleaseOrchestratorStorageProperties(
    String artefatosDir,
    String entregasDir,
    Integer retencaoDias) {

  public ReleaseOrchestratorStorageProperties {
    if (retencaoDias == null) retencaoDias = 90;
  }
}
