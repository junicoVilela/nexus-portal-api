package br.com.softon.portal.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docflow.storage")
public record StorageProperties(String publicacoesDir) {
}
