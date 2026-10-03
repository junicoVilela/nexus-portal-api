package com.nexus.portal.ai.config;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fase C: PR mergeado no GitHub vira proposta na fila. Runbook em {@code docs/ai/GITHUB-WEBHOOK.md}.
 *
 * <pre>
 * NEXUS_AI_GITHUB_WEBHOOK_SECRET=...           # o mesmo "Secret" do webhook no GitHub
 * NEXUS_AI_GITHUB_TOKEN=...                    # leitura de PRs (repositório privado)
 * NEXUS_AI_GITHUB_REPOSITORIOS_0_NOME=org/app
 * NEXUS_AI_GITHUB_REPOSITORIOS_0_PROJETO_ID=…  # projeto DocFlow das páginas novas
 * NEXUS_AI_GITHUB_REPOSITORIOS_0_MODULO_ID=…   # módulo padrão das páginas novas
 * </pre>
 *
 * <p>Sem {@code webhookSecret} o endpoint recusa tudo (403). Repositório fora da lista é ignorado.
 */
@ConfigurationProperties(prefix = "nexus.ai.github")
public record AiGithubProperties(
    String webhookSecret,
    String token,
    String apiUrl,
    List<String> branches,
    List<Repositorio> repositorios,
    /** Globs de arquivos de tela; o resto conta como backend. */
    List<String> caminhosTela,
    /** Rótulo do PR que manda ignorar (ex.: refactor sem efeito na tela). */
    String rotuloIgnorar,
    int maxArquivos,
    int maxCaracteresPatch) {

  public static final String API_GITHUB = "https://api.github.com";
  public static final List<String> CAMINHOS_TELA_PADRAO = List.of(
      "**/*.component.html", "**/*.component.ts", "**/*.component.css", "**/*.component.scss",
      "**/pages/**", "**/views/**", "**/*.tsx", "**/*.jsx", "**/*.vue");

  public AiGithubProperties {
    apiUrl = apiUrl == null || apiUrl.isBlank() ? API_GITHUB : apiUrl.replaceAll("/+$", "");
    branches = branches == null || branches.isEmpty() ? List.of("main") : List.copyOf(branches);
    repositorios = repositorios == null ? List.of() : List.copyOf(repositorios);
    caminhosTela = caminhosTela == null || caminhosTela.isEmpty() ? CAMINHOS_TELA_PADRAO : List.copyOf(caminhosTela);
    rotuloIgnorar = rotuloIgnorar == null || rotuloIgnorar.isBlank() ? "docs:skip" : rotuloIgnorar.strip();
    maxArquivos = maxArquivos <= 0 ? 60 : maxArquivos;
    maxCaracteresPatch = maxCaracteresPatch <= 0 ? 12_000 : maxCaracteresPatch;
  }

  public boolean webhookConfigurado() {
    return webhookSecret != null && !webhookSecret.isBlank();
  }

  public Optional<Repositorio> repositorio(String nome) {
    return repositorios.stream().filter(r -> r.nome() != null && r.nome().equalsIgnoreCase(nome)).findFirst();
  }

  /** Repositório acompanhado e onde ficam as páginas novas que ele gerar. */
  public record Repositorio(String nome, UUID projetoId, UUID moduloId) {}
}
