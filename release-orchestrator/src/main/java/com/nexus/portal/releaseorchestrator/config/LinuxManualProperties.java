package com.nexus.portal.releaseorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "release-orchestrator.linux-manual")
public record LinuxManualProperties(String templateDir, Boolean allowLocalFs) {

  public LinuxManualProperties {
    if (allowLocalFs == null) {
      allowLocalFs = false;
    }
  }

  public boolean localFsHabilitado() {
    return Boolean.TRUE.equals(allowLocalFs);
  }

  public boolean temTemplate() {
    return templateDir != null && !templateDir.isBlank();
  }
}
