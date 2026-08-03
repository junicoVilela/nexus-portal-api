package com.nexus.portal.releaseorchestrator.health;

import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubException;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
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
 * Health da integração GitHub — itera produtos ativos com integração e
 * faz uma chamada de prova ({@code listarReleases} com limite 1) por produto.
 *
 * <p>Cache de {@value #TTL_MINUTOS} minutos para não bater na API toda vez
 * que um uptime monitor consulta {@code /actuator/health}. Cada produto
 * acessível conta como UP; falhas individuais aparecem em detalhes mas o
 * status agregado fica UP se ao menos um produto responde.
 *
 * <p>Sem produtos com GitHub configurado → status UP com {@code produtos: 0}
 * (não é um erro, só não há integração ativa).
 */
@Slf4j
@Component("releaseOrchestratorGithubHealthIndicator")
@RequiredArgsConstructor
public class GithubHealthIndicator implements HealthIndicator {

  private static final long TTL_MINUTOS = 1;

  private final ProdutoRhRepository produtoRepository;
  private final GitHubReleasesAdapter adapter;

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
        .filter(ProdutoRh::temIntegracaoGithub)
        .toList();

    if (produtos.isEmpty()) {
      return Health.up().withDetail("produtos", 0).build();
    }

    Map<String, Object> detalhes = new LinkedHashMap<>();
    int ok = 0;
    int falhou = 0;
    for (ProdutoRh produto : produtos) {
      try {
        adapter.listarReleases(produto.getRepositorioGithub(), produto.getGithubToken(), 1);
        detalhes.put(produto.getSigla(), "OK (" + produto.getRepositorioGithub() + ")");
        ok++;
      } catch (GitHubException e) {
        detalhes.put(produto.getSigla(), "FALHA: " + e.getMessage());
        falhou++;
      } catch (RuntimeException e) {
        log.warn("Erro inesperado no health GitHub do produto {}: {}",
            produto.getSigla(), e.getMessage());
        detalhes.put(produto.getSigla(), "ERRO: " + e.getMessage());
        falhou++;
      }
    }
    detalhes.put("produtos", produtos.size());
    detalhes.put("acessiveis", ok);
    detalhes.put("comFalha", falhou);

    // UP se pelo menos um produto responde; DOWN se todos falham.
    Health.Builder builder = (ok > 0) ? Health.up() : Health.down();
    detalhes.forEach(builder::withDetail);
    return builder.build();
  }
}
