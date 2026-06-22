package br.com.softon.portal.releaseorchestrator.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record TestarGithubResponse(
    boolean sucesso,
    String repositorio,
    /** Mensagem amigável de erro, quando sucesso=false. */
    String erro,
    /** Releases mais recentes (até 5), null quando sucesso=false. */
    List<ReleaseResumo> releasesRecentes) {

  public record ReleaseResumo(String tagName, String name, OffsetDateTime publishedAt,
      int totalAssets) {}

  public static TestarGithubResponse ok(String repositorio, List<ReleaseResumo> releases) {
    return new TestarGithubResponse(true, repositorio, null, releases);
  }

  public static TestarGithubResponse erro(String repositorio, String mensagem) {
    return new TestarGithubResponse(false, repositorio, mensagem, null);
  }
}
