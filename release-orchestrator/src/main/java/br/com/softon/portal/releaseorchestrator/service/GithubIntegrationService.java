package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.TestarGithubRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.TestarGithubResponse;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubException;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubRelease;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orquestração da integração GitHub no nível do Produto. Faz a ponte entre
 * o cadastro do produto e o {@link GitHubReleasesAdapter}.
 */
@Service
@RequiredArgsConstructor
public class GithubIntegrationService {

  private final ProdutoRhService produtoService;
  private final GitHubReleasesAdapter adapter;

  /**
   * Testa a integração GitHub para um produto. Se o request trouxer override
   * de repo/token, usa essas credenciais; caso contrário, usa as do produto.
   *
   * Resposta nunca lança — encapsula falha em {@link TestarGithubResponse#sucesso()}.
   */
  public TestarGithubResponse testar(UUID produtoId, TestarGithubRequest req) {
    ProdutoRh produto = produtoService.buscar(produtoId);
    String repo = req.temOverride() ? req.repositorioGithub() : produto.getRepositorioGithub();
    String token = (req.githubToken() != null && !req.githubToken().isBlank())
        ? req.githubToken()
        : produto.getGithubToken();

    if (repo == null || repo.isBlank()) {
      return TestarGithubResponse.erro(null,
          "Configure o repositório antes de testar.");
    }
    if (token == null || token.isBlank()) {
      return TestarGithubResponse.erro(repo,
          "Configure um token GitHub antes de testar.");
    }

    try {
      List<GitHubRelease> releases = adapter.listarReleases(repo, token, 5);
      List<TestarGithubResponse.ReleaseResumo> resumos = releases.stream()
          .map(r -> new TestarGithubResponse.ReleaseResumo(
              r.tagName(), r.name(), r.publishedAt(),
              r.assets() == null ? 0 : r.assets().size()))
          .toList();
      return TestarGithubResponse.ok(repo, resumos);
    } catch (GitHubException e) {
      return TestarGithubResponse.erro(repo, e.getMessage());
    }
  }
}
