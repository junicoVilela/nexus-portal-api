package br.com.softon.portal.releaseorchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do retry de publicação remota (F3 P2).
 *
 * <ul>
 *   <li>{@code max-tentativas}: limite de tentativas antes de marcar
 *       FALHA definitivo. Default 5.</li>
 *   <li>{@code backoff-inicial-minutos}: intervalo da 1ª retentativa.
 *       Default 5 min. Backoff exponencial: 5, 10, 20, 40, 80 min.</li>
 *   <li>{@code backoff-max-minutos}: teto do intervalo entre tentativas.
 *       Default 240 min (4 h).</li>
 *   <li>{@code retry-cron}: cron do job que varre PENDENTES vencidas.
 *       Default {@code "0 *&#47;2 * * * *"} (a cada 2 min).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "release-orchestrator.publicacao")
public record ReleaseOrchestratorPublicacaoProperties(
    Integer maxTentativas,
    Integer backoffInicialMinutos,
    Integer backoffMaxMinutos,
    String retryCron) {

  public ReleaseOrchestratorPublicacaoProperties {
    if (maxTentativas == null || maxTentativas <= 0) maxTentativas = 5;
    if (backoffInicialMinutos == null || backoffInicialMinutos <= 0) backoffInicialMinutos = 5;
    if (backoffMaxMinutos == null || backoffMaxMinutos <= 0) backoffMaxMinutos = 240;
    if (retryCron == null || retryCron.isBlank()) retryCron = "0 */2 * * * *";
  }
}
