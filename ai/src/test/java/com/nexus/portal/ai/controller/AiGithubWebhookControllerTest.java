package com.nexus.portal.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiGithubProperties;
import com.nexus.portal.ai.integration.github.AiGithubAssinatura;
import com.nexus.portal.ai.service.AiPrIngestaoService;
import com.nexus.portal.ai.service.AiPrIngestaoService.PullRequestMergeado;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiGithubWebhookControllerTest {

  private static final String SECRET = "segredo";
  private final AiPrIngestaoService ingestao = mock(AiPrIngestaoService.class);

  private AiGithubWebhookController controller(String secret) {
    return new AiGithubWebhookController(
        new AiGithubProperties(secret, null, null, List.of("main"), List.of(), null, null, 0, 0),
        ingestao, new ObjectMapper());
  }

  private static byte[] payload(String action, boolean merged, String base) {
    return ("{\"action\":\"" + action + "\",\"repository\":{\"full_name\":\"org/app\"},"
        + "\"pull_request\":{\"number\":42,\"title\":\"Tela PED-001\",\"body\":null,\"merged\":" + merged
        + ",\"html_url\":\"https://github.com/org/app/pull/42\",\"user\":{\"login\":\"dev\"},"
        + "\"base\":{\"ref\":\"" + base + "\"},\"merge_commit_sha\":\"abc\",\"merged_at\":\"2026-10-01T12:00:00Z\","
        + "\"labels\":[{\"name\":\"frontend\"}]}}").getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void semSecretConfiguradoOuAssinaturaErradaRecusa() throws Exception {
    byte[] corpo = payload("closed", true, "main");
    assertThat(controller("").receber("pull_request", "d1", AiGithubAssinatura.assinar("x", corpo), corpo)
        .getStatusCode().value()).isEqualTo(403);
    assertThat(controller(SECRET).receber("pull_request", "d1", AiGithubAssinatura.assinar("x", corpo), corpo)
        .getStatusCode().value()).isEqualTo(403);
    verify(ingestao, never()).receber(any());
  }

  @Test
  void prMergeadoNaBranchAcompanhadaEntraNaFila() throws Exception {
    byte[] corpo = payload("closed", true, "main");
    UUID id = UUID.randomUUID();
    when(ingestao.receber(any())).thenReturn(Optional.of(id));

    var resposta = controller(SECRET).receber("pull_request", "d1", AiGithubAssinatura.assinar(SECRET, corpo), corpo);

    assertThat(resposta.getStatusCode().value()).isEqualTo(202);
    ArgumentCaptor<PullRequestMergeado> pr = ArgumentCaptor.forClass(PullRequestMergeado.class);
    verify(ingestao).receber(pr.capture());
    assertThat(pr.getValue().repositorio()).isEqualTo("org/app");
    assertThat(pr.getValue().numero()).isEqualTo(42);
    assertThat(pr.getValue().corpo()).isNull();
    assertThat(pr.getValue().rotulos()).containsExactly("frontend");
    verify(ingestao).agendar(id);
  }

  @Test
  void pingFechadoSemMergeOutraBranchEReentregaNaoTemEfeito() throws Exception {
    var controller = controller(SECRET);
    byte[] ping = "{\"zen\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
    assertThat(controller.receber("ping", "d0", AiGithubAssinatura.assinar(SECRET, ping), ping)
        .getStatusCode().value()).isEqualTo(204);
    for (byte[] corpo : List.of(payload("closed", false, "main"), payload("closed", true, "develop"),
        payload("opened", false, "main"))) {
      assertThat(controller.receber("pull_request", "d2", AiGithubAssinatura.assinar(SECRET, corpo), corpo)
          .getStatusCode().value()).isEqualTo(204);
    }
    verify(ingestao, never()).receber(any());

    byte[] corpo = payload("closed", true, "main");
    when(ingestao.receber(any())).thenReturn(Optional.empty());
    assertThat(controller.receber("pull_request", "d3", AiGithubAssinatura.assinar(SECRET, corpo), corpo)
        .getStatusCode().value()).isEqualTo(204);
    verify(ingestao, never()).agendar(any());
  }
}
