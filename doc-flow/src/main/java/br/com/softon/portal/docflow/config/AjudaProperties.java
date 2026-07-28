package br.com.softon.portal.docflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docflow.ajuda")
public record AjudaProperties(Integer retencaoEventosDias) {

  public AjudaProperties {
    if (retencaoEventosDias == null) retencaoEventosDias = 180;
  }
}
