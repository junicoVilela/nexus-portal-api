package com.nexus.portal.docflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param timeoutMinutos tempo máximo que uma publicação pode ficar em GERANDO
 *     antes do watchdog considerá-la interrompida.
 */
@ConfigurationProperties(prefix = "docflow.publicacao")
public record PublicacaoProperties(Integer timeoutMinutos) {

  public PublicacaoProperties {
    if (timeoutMinutos == null || timeoutMinutos <= 0) timeoutMinutos = 30;
  }
}
