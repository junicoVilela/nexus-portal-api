package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.response.ReleaseDisponivelDeployResponse;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubException;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Releases usáveis no deploy: portal (em andamento, aprovada, publicada) + tags
 * do GitHub quando o produto tem integração.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseDisponivelDeployService {

  static final Set<ReleaseStatus> IMPLANTAVEIS = EnumSet.of(
      ReleaseStatus.EM_DESENVOLVIMENTO,
      ReleaseStatus.EM_REVISAO,
      ReleaseStatus.APROVADA,
      ReleaseStatus.PUBLICADA);

  private final ReleaseRepository releaseRepository;
  private final ProdutoRhService produtoService;
  private final GitHubReleasesAdapter gitHub;

  @Transactional(readOnly = true)
  public List<ReleaseDisponivelDeployResponse> listar(UUID produtoId) {
    ProdutoRh produto = produtoService.buscar(produtoId);
    List<Release> portal = releaseRepository.findAll(
        (root, q, cb) -> cb.and(
            cb.equal(root.get("produto").get("id"), produtoId),
            root.get("status").in(IMPLANTAVEIS)),
        Sort.by(Sort.Direction.DESC, "updatedAt"));
    Map<String, GitHubRelease> gitPorVersao = carregarGit(produto);
    Map<String, ReleaseDisponivelDeployResponse> porVersao = new LinkedHashMap<>();
    for (Release r : portal) {
      String chave = normalizar(r.getVersao());
      GitHubRelease git = gitPorVersao.remove(chave);
      boolean andamento = r.getStatus() == ReleaseStatus.EM_DESENVOLVIMENTO
          || r.getStatus() == ReleaseStatus.EM_REVISAO;
      porVersao.put(chave, new ReleaseDisponivelDeployResponse(
          r.getId(), r.getVersao(), r.getTitulo(), r.getStatus(),
          true, git != null, andamento,
          git != null && git.draft(), git != null && git.prerelease(),
          git == null ? null : git.tagName()));
    }
    for (GitHubRelease git : gitPorVersao.values()) {
      String versao = git.tagName() == null ? "" : git.tagName();
      String titulo = git.name() == null || git.name().isBlank() ? versao : git.name();
      boolean andamento = git.draft() || git.prerelease();
      porVersao.put(normalizar(versao), new ReleaseDisponivelDeployResponse(
          null, versao, titulo, null, false, true, andamento,
          git.draft(), git.prerelease(), git.tagName()));
    }
    List<ReleaseDisponivelDeployResponse> out = new ArrayList<>(porVersao.values());
    out.sort(Comparator
        .comparing((ReleaseDisponivelDeployResponse r) -> !r.selecionavel())
        .thenComparing(r -> !r.emAndamento())
        .thenComparing(ReleaseDisponivelDeployResponse::versao, Comparator.reverseOrder()));
    return out;
  }

  private Map<String, GitHubRelease> carregarGit(ProdutoRh produto) {
    Map<String, GitHubRelease> map = new LinkedHashMap<>();
    if (!produto.temIntegracaoGithub()) {
      return map;
    }
    try {
      for (GitHubRelease git : gitHub.listarReleases(
          produto.getRepositorioGithub(), produto.getGithubToken(), 40, true)) {
        if (git.tagName() == null || git.tagName().isBlank()) {
          continue;
        }
        map.putIfAbsent(normalizar(git.tagName()), git);
      }
    } catch (GitHubException ex) {
      log.warn("Não listou releases GitHub de {}: {}", produto.getRepositorioGithub(), ex.getMessage());
    }
    return map;
  }

  static String normalizar(String versao) {
    if (versao == null) {
      return "";
    }
    String t = versao.trim().toLowerCase(Locale.ROOT);
    if (t.startsWith("v") && t.length() > 1 && Character.isDigit(t.charAt(1))) {
      return t.substring(1);
    }
    return t;
  }
}
