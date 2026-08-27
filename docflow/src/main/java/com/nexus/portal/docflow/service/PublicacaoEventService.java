package com.nexus.portal.docflow.service;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.docflow.entity.Publicacao;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Stream de eventos de publicação. Cada assinante recebe apenas os clientes do
 * seu escopo — sem isso um usuário restrito a um cliente veria nome e versão
 * das publicações de todos os outros.
 */
@Service
@RequiredArgsConstructor
public class PublicacaoEventService {

  private final EscopoResolver escopoResolver;
  private final SseBroadcaster broadcaster = new SseBroadcaster();

  public SseEmitter inscrever() {
    return broadcaster.inscrever(escopoResolver.clientesPermitidosDoUsuarioAtual());
  }

  public void publicar(Publicacao publicacao) {
    Map<String, Object> evento = Map.of(
        "id", publicacao.getId(),
        "clienteId", publicacao.getCliente().getId(),
        "status", publicacao.getStatus().name(),
        "versao", publicacao.getVersao());
    broadcaster.publicar("publicacao", evento, publicacao.getCliente().getId());
  }
}
