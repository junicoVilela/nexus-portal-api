package com.nexus.portal.ai.controller;

import com.nexus.portal.ai.dto.request.AiManualPerguntaRequest;
import com.nexus.portal.ai.dto.response.AiManualRespostaResponse;
import com.nexus.portal.ai.service.AiManualPerguntaService;
import com.nexus.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perguntar ao manual publicado (Onda E). Só lê o snapshot da publicação: rascunhos e edições
 * posteriores nunca entram na resposta.
 */
@RestController
public class AiManualController {

  private final AiManualPerguntaService service;

  public AiManualController(AiManualPerguntaService service) {
    this.service = service;
  }

  /** Pelo portal, sobre qualquer publicação concluída. */
  @PostMapping("/api/v1/ai/publicacoes/{id}/perguntar")
  @PreAuthorize(Permissoes.PUBLICACAO_LER)
  public AiManualRespostaResponse perguntarPublicacao(
      @PathVariable UUID id, @Valid @RequestBody AiManualPerguntaRequest request) {
    return service.perguntarPublicacao(id, request.pergunta());
  }

  /**
   * Pelo leitor (prévia online, app do cliente): público no {@code SecurityConfig}; o token de
   * prévia identifica o cliente e limita os acessos. Responde sobre o manual vigente dele.
   */
  @PostMapping("/api/v1/ai/manual/{token}/perguntar")
  public AiManualRespostaResponse perguntarComToken(
      @PathVariable String token,
      @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origem,
      @Valid @RequestBody AiManualPerguntaRequest request) {
    return service.perguntarComToken(token, origem, request.pergunta());
  }
}
