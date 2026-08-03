package com.nexus.portal.releaseorchestrator.health;

import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsAdapter;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsException;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health da integração Jenkins — mesma estratégia do
 * {@link GithubHealthIndicator}: itera produtos ativos com integração e
 * faz {@code jobExiste} por produto, com cache de
 * {@value #TTL_MINUTOS} minutos.
 */
@Slf4j
@Component("releaseOrchestratorJenkinsHealthIndicator")
@RequiredArgsConstructor
public class JenkinsHealthIndicator implements HealthIndicator {

  private static final long TTL_MINUTOS = 1;

  private final ProdutoRhRepository produtoRepository;
  private final JenkinsAdapter adapter;

  private volatile Health cache;
  private volatile Instant cacheAt = Instant.EPOCH;

  @Override
  public Health health() {
    if (cache != null
        && Duration.between(cacheAt, Instant.now()).toMinutes() < TTL_MINUTOS) {
      return cache;
    }
    Health resultado = avaliar();
    cache = resultado;
    cacheAt = Instant.now();
    return resultado;
  }

  private Health avaliar() {
    List<ProdutoRh> produtos = produtoRepository.findAll().stream()
        .filter(ProdutoRh::isAtivo)
        .filter(ProdutoRh::temIntegracaoJenkins)
        .toList();

    if (produtos.isEmpty()) {
      return Health.up().withDetail("produtos", 0).build();
    }

    Map<String, Object> detalhes = new LinkedHashMap<>();
    int ok = 0;
    int falhou = 0;
    for (ProdutoRh produto : produtos) {
      try {
        boolean existe = adapter.jobExiste(produto.getJenkinsUrl(), produto.getJenkinsJob(),
            produto.getJenkinsUser(), produto.getJenkinsToken());
        if (existe) {
          detalhes.put(produto.getSigla(), "OK (" + produto.getJenkinsJob() + ")");
          ok++;
        } else {
          detalhes.put(produto.getSigla(), "job não encontrado: " + produto.getJenkinsJob());
          falhou++;
        }
      } catch (JenkinsException e) {
        detalhes.put(produto.getSigla(), "FALHA: " + e.getMessage());
        falhou++;
      } catch (RuntimeException e) {
        log.warn("Erro inesperado no health Jenkins do produto {}: {}",
            produto.getSigla(), e.getMessage());
        detalhes.put(produto.getSigla(), "ERRO: " + e.getMessage());
        falhou++;
      }
    }
    detalhes.put("produtos", produtos.size());
    detalhes.put("acessiveis", ok);
    detalhes.put("comFalha", falhou);

    Health.Builder builder = (ok > 0) ? Health.up() : Health.down();
    detalhes.forEach(builder::withDetail);
    return builder.build();
  }
}
