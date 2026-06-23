package br.com.softon.portal.releaseorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração dos webhooks recebidos do Jenkins/GitHub.
 *
 * <p>{@code jenkinsSecret} é um shared secret simples — o Jenkins envia o
 * valor no header {@code X-Webhook-Secret} e o controller compara em tempo
 * constante. Vazio/nulo desabilita o endpoint (retorna 403).
 *
 * Spec: docs/jornadas/02-checklist-por-sprint.md S11.
 */
@ConfigurationProperties(prefix = "release-orchestrator.webhooks")
public record ReleaseOrchestratorWebhookProperties(String jenkinsSecret) {
}
