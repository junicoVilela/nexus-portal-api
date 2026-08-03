package com.nexus.portal.ai.service;

import com.nexus.portal.ai.config.AiProperties;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rate limit simples em memória por usuário (MVP interno).
 * Conta gerações ({@code POST .../gerar}) na janela de 1 hora.
 */
@Service
public class AiRateLimitService {

  private static final long JANELA_MS = 60L * 60L * 1000L;

  private final AiProperties properties;
  private final Map<String, Deque<Long>> geracoesPorUsuario = new ConcurrentHashMap<>();

  public AiRateLimitService(AiProperties properties) {
    this.properties = properties;
  }

  public void exigirGeracaoPermitida(Principal principal) {
    int max = properties.maxGeracoesPorHora();
    if (max <= 0) {
      return;
    }
    String key = principal == null ? "anonymous" : principal.getName();
    long agora = Instant.now().toEpochMilli();
    Deque<Long> fila = geracoesPorUsuario.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (fila) {
      while (!fila.isEmpty() && agora - fila.peekFirst() > JANELA_MS) {
        fila.pollFirst();
      }
      if (fila.size() >= max) {
        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Limite de gerações AI atingido (" + max + "/hora). Aguarde e tente novamente.");
      }
      fila.addLast(agora);
    }
  }

  /** Só para testes. */
  void limpar() {
    geracoesPorUsuario.clear();
  }
}
