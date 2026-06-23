package br.com.softon.portal.releaseorchestrator.controller;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorWebhookProperties;
import br.com.softon.portal.releaseorchestrator.dto.request.JenkinsWebhookRequest;
import br.com.softon.portal.releaseorchestrator.service.JenkinsWebhookService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe notificações do Jenkins via webhook (S11 P2). Endpoint público
 * (permitAll no SecurityConfig) protegido por shared secret no header
 * {@code X-Webhook-Secret}.
 *
 * <p>Quando {@code release-orchestrator.webhooks.jenkins-secret} não está
 * configurado, qualquer chamada retorna 403 — o endpoint só entra em
 * operação quando o secret é definido em prod (env var ou yml).
 *
 * <p>Comparação em tempo constante para evitar timing attacks.
 *
 * <p>Resposta:
 * <ul>
 *   <li>204 No Content em sucesso (Jenkins descarta o body)</li>
 *   <li>403 quando o secret está ausente, mal configurado ou não confere</li>
 *   <li>404 quando a release não existe (vira log warn no service)</li>
 *   <li>422 quando o status é inválido</li>
 * </ul>
 */
@Slf4j
@RestController("orchestratorJenkinsWebhookController")
@RequestMapping("/api/v1/release-orchestrator/webhooks/jenkins")
@RequiredArgsConstructor
public class JenkinsWebhookController {

  private static final String HEADER_SECRET = "X-Webhook-Secret";

  private final JenkinsWebhookService service;
  private final ReleaseOrchestratorWebhookProperties webhookProperties;

  @PostMapping
  public ResponseEntity<Void> receber(
      @RequestHeader(value = HEADER_SECRET, required = false) String secretRecebido,
      @Valid @RequestBody JenkinsWebhookRequest request) {

    String esperado = webhookProperties.jenkinsSecret();
    if (esperado == null || esperado.isBlank()) {
      log.warn("Webhook Jenkins chegou mas release-orchestrator.webhooks.jenkins-secret "
          + "não está configurado — rejeitando.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (secretRecebido == null || !secretsConferem(esperado, secretRecebido)) {
      log.warn("Webhook Jenkins com secret ausente/inválido — rejeitando.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    service.processar(request);
    return ResponseEntity.noContent().build();
  }

  /** Constant-time comparison para evitar timing attacks no shared secret. */
  private boolean secretsConferem(String esperado, String recebido) {
    byte[] a = esperado.getBytes(StandardCharsets.UTF_8);
    byte[] b = recebido.getBytes(StandardCharsets.UTF_8);
    if (a.length != b.length) return false;
    int diff = 0;
    for (int i = 0; i < a.length; i++) {
      diff |= a[i] ^ b[i];
    }
    return diff == 0;
  }
}
