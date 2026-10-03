package com.nexus.portal.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiGithubProperties;
import com.nexus.portal.ai.integration.github.AiGithubAssinatura;
import com.nexus.portal.ai.service.AiPrIngestaoService;
import com.nexus.portal.ai.service.AiPrIngestaoService.PullRequestMergeado;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook do GitHub (AI-601). Público no {@code SecurityConfig}; a autenticação é a assinatura
 * {@code X-Hub-Signature-256} (HMAC do corpo com o "Secret" configurado no GitHub).
 *
 * <ul>
 *   <li>403 — secret não configurado ou assinatura inválida;</li>
 *   <li>202 — PR mergeado aceito para a fila;</li>
 *   <li>204 — evento válido sem efeito (ping, PR fechado sem merge, outra branch, reentrega).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai/webhooks/github")
public class AiGithubWebhookController {

  private static final Logger log = LoggerFactory.getLogger(AiGithubWebhookController.class);

  private final AiGithubProperties properties;
  private final AiPrIngestaoService ingestaoService;
  private final ObjectMapper objectMapper;

  public AiGithubWebhookController(
      AiGithubProperties properties, AiPrIngestaoService ingestaoService, ObjectMapper objectMapper) {
    this.properties = properties;
    this.ingestaoService = ingestaoService;
    this.objectMapper = objectMapper;
  }

  @PostMapping
  public ResponseEntity<Void> receber(
      @RequestHeader(value = "X-GitHub-Event", required = false) String evento,
      @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String assinatura,
      @RequestBody(required = false) byte[] corpo) throws IOException {
    if (!properties.webhookConfigurado()) {
      log.warn("ai.github.webhook recusado: nexus.ai.github.webhook-secret não configurado.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    byte[] bytes = corpo == null ? new byte[0] : corpo;
    if (!AiGithubAssinatura.valida(properties.webhookSecret(), bytes, assinatura)) {
      log.warn("ai.github.webhook recusado: assinatura ausente ou inválida (delivery={}).", deliveryId);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    if (!"pull_request".equals(evento) || deliveryId == null || deliveryId.isBlank()) {
      return ResponseEntity.noContent().build();
    }
    JsonNode payload = objectMapper.readTree(bytes);
    JsonNode pr = payload.path("pull_request");
    String base = pr.path("base").path("ref").asText();
    if (!"closed".equals(payload.path("action").asText()) || !pr.path("merged").asBoolean()
        || !properties.branches().contains(base)) {
      return ResponseEntity.noContent().build();
    }
    List<String> rotulos = new ArrayList<>();
    pr.path("labels").forEach(rotulo -> rotulos.add(rotulo.path("name").asText()));
    var recebido = ingestaoService.receber(new PullRequestMergeado(
        deliveryId,
        payload.path("repository").path("full_name").asText(),
        pr.path("number").asInt(),
        pr.path("title").asText(),
        pr.path("body").isNull() ? null : pr.path("body").asText(null),
        rotulos,
        pr.path("html_url").asText(),
        pr.path("user").path("login").asText(null),
        base,
        pr.path("merge_commit_sha").asText(null),
        pr.hasNonNull("merged_at") ? OffsetDateTime.parse(pr.get("merged_at").asText()) : null));
    if (recebido.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    ingestaoService.agendar(recebido.get());
    return ResponseEntity.accepted().build();
  }
}
