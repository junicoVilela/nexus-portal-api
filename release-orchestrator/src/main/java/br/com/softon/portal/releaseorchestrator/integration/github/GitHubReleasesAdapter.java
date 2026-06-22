package br.com.softon.portal.releaseorchestrator.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adapter para a API REST do GitHub Releases (F2.1). MVP cobre:
 *
 * <ul>
 *   <li>Listar releases de um repositório (para a tela de teste de conexão).</li>
 *   <li>Buscar release por tag.</li>
 *   <li>Download streaming de asset por nome.</li>
 * </ul>
 *
 * Spec: docs/release-orchestrator/09-produtos-cadastro.md §4.2
 *       docs/release-orchestrator/39-entregaveis-cicd-repositorios.md
 *
 * Erros são embrulhados em {@link GitHubException} para tratamento uniforme.
 * Não persiste estado — service de cache de download fica acima.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubReleasesAdapter {

  private static final String BASE_URL = "https://api.github.com";
  private static final String ACCEPT = "application/vnd.github+json";
  private static final String API_VERSION = "2022-11-28";

  private final ObjectMapper objectMapper;

  /**
   * Retorna até {@code limite} releases do repositório, ordenadas pelo GitHub
   * (mais recente primeiro). Não inclui drafts.
   *
   * @throws GitHubException se a credencial for inválida ou o repo não existir.
   */
  public List<GitHubRelease> listarReleases(String repositorio, String token, int limite) {
    int safe = Math.min(Math.max(1, limite), 100);
    String url = String.format("/repos/%s/releases?per_page=%d", repositorio, safe);
    GitHubReleaseDto[] dtos = executar(token, url, GitHubReleaseDto[].class);
    return Arrays.stream(dtos)
        .filter(d -> !d.draft())
        .map(GitHubReleaseDto::toDomain)
        .sorted(Comparator.comparing(
            GitHubRelease::publishedAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  /** Retorna a release de uma tag específica, se existir. */
  public Optional<GitHubRelease> buscarPorTag(String repositorio, String tagName, String token) {
    try {
      String url = String.format("/repos/%s/releases/tags/%s", repositorio, tagName);
      GitHubReleaseDto dto = executar(token, url, GitHubReleaseDto.class);
      return Optional.of(dto.toDomain());
    } catch (GitHubException e) {
      if (e.status() == 404) return Optional.empty();
      throw e;
    }
  }

  /**
   * Baixa um asset do GitHub. Caller é responsável por fechar o stream.
   *
   * @throws GitHubException 404 se o asset não existir.
   */
  public InputStream baixarAsset(String repositorio, long assetId, String token) {
    String url = String.format("/repos/%s/releases/assets/%d", repositorio, assetId);
    RestClient client = clienteBase(token);
    try {
      return client.get()
          .uri(url)
          .accept(MediaType.APPLICATION_OCTET_STREAM)
          .retrieve()
          .body(InputStream.class);
    } catch (HttpStatusCodeException e) {
      throw new GitHubException(
          "Falha ao baixar asset " + assetId + " de " + repositorio,
          e.getStatusCode().value(), e);
    }
  }

  private <T> T executar(String token, String uri, Class<T> tipo) {
    RestClient client = clienteBase(token);
    try {
      return client.get().uri(uri).retrieve().body(tipo);
    } catch (HttpStatusCodeException e) {
      String mensagem = mensagemAmigavel(e);
      throw new GitHubException(mensagem, e.getStatusCode().value(), e);
    } catch (RestClientResponseException e) {
      throw new GitHubException("Resposta inesperada do GitHub: " + e.getMessage(),
          e.getStatusCode().value(), e);
    }
  }

  private RestClient clienteBase(String token) {
    return RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.ACCEPT, ACCEPT)
        .defaultHeader("X-GitHub-Api-Version", API_VERSION)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
  }

  private String mensagemAmigavel(HttpStatusCodeException e) {
    int status = e.getStatusCode().value();
    return switch (status) {
      case 401 -> "Token GitHub inválido ou expirado.";
      case 403 -> "Token sem permissão para acessar este repositório (escopo `repo`).";
      case 404 -> "Repositório ou recurso não encontrado.";
      default -> "GitHub retornou erro " + status + ".";
    };
  }

  // ------- DTOs internos (mapeamento JSON) -------

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GitHubReleaseDto(
      @JsonProperty("tag_name") String tagName,
      String name,
      boolean draft,
      boolean prerelease,
      @JsonProperty("published_at") OffsetDateTime publishedAt,
      GitHubAssetDto[] assets) {

    GitHubRelease toDomain() {
      List<GitHubRelease.Asset> dominio = assets == null ? List.of()
          : Arrays.stream(assets).map(GitHubAssetDto::toDomain).toList();
      return new GitHubRelease(tagName, name, draft, prerelease, publishedAt, dominio);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GitHubAssetDto(
      Long id,
      String name,
      Long size,
      @JsonProperty("content_type") String contentType,
      @JsonProperty("browser_download_url") String browserDownloadUrl) {

    GitHubRelease.Asset toDomain() {
      return new GitHubRelease.Asset(id, name, size, contentType, browserDownloadUrl);
    }
  }
}
