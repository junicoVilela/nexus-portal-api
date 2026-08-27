package com.nexus.portal.releaseorchestrator.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clones Git locais para listar tags/branches sem PAT do GitHub (lab).
 *
 * <pre>
 * release-orchestrator.git.local-clones:
 *   - repositorio: softonsi/dtec-ld
 *     caminho: /caminho/dtec-ld
 * </pre>
 */
@ConfigurationProperties(prefix = "release-orchestrator.git")
public record LocalGitProperties(List<Clone> localClones) {

  public LocalGitProperties {
    localClones = localClones == null ? List.of() : List.copyOf(localClones);
  }

  public record Clone(String repositorio, String caminho) {}
}
