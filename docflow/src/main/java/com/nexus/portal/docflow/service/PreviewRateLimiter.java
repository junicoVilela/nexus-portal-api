package com.nexus.portal.docflow.service;

import com.nexus.portal.shared.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limita acessos ao preview público por token. O endpoint é {@code permitAll} e
 * remonta o manual do cliente; sem limite, um token vazado vira um jeito barato
 * de consumir CPU do servidor.
 *
 * <p>Janela fixa por token, contada em memória — vale por instância. É proteção
 * contra abuso trivial, não substitui limite na borda (proxy/WAF).
 */
@Component
public class PreviewRateLimiter {

  private final Map<String, Janela> janelasPorToken = new ConcurrentHashMap<>();

  @Value("${docflow.preview.limite-por-minuto:30}")
  private int limitePorMinuto;

  public void registrarAcesso(String token) {
    if (limitePorMinuto <= 0) {
      return;
    }
    Instant agora = Instant.now();
    Janela janela = janelasPorToken.compute(token, (chave, atual) ->
        atual == null || atual.expirada(agora) ? new Janela(agora.plus(Duration.ofMinutes(1))) : atual);

    if (janela.acessos().incrementAndGet() > limitePorMinuto) {
      throw new BusinessException(
          "Muitos acessos a este preview. Tente novamente em instantes.");
    }
    limparExpiradas(agora);
  }

  /** A limpeza é oportunista: sem ela o mapa cresceria com tokens já revogados. */
  private void limparExpiradas(Instant agora) {
    if (janelasPorToken.size() > 1_000) {
      janelasPorToken.entrySet().removeIf(entrada -> entrada.getValue().expirada(agora));
    }
  }

  private record Janela(Instant fim, AtomicInteger acessos) {

    Janela(Instant fim) {
      this(fim, new AtomicInteger());
    }

    boolean expirada(Instant agora) {
      return agora.isAfter(fim);
    }
  }
}
