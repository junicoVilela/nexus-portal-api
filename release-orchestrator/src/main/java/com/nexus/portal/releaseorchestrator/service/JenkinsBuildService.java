package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.DispararBuildRequest;
import com.nexus.portal.releaseorchestrator.dto.request.OrigemBuild;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesBuildResponse.FonteBuild;
import com.nexus.portal.releaseorchestrator.dto.response.FontesBuildResponse.TagBuild;
import com.nexus.portal.releaseorchestrator.entity.AcaoHistorico;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseHistorico;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.integration.git.LocalGitCatalog;
import com.nexus.portal.releaseorchestrator.integration.git.LocalGitCatalog.LocalGitRef;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubException;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubTag;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsAdapter;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsException;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsQueueInfo;
import com.nexus.portal.releaseorchestrator.repository.ReleaseHistoricoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispara o job Jenkins do produto a partir da ficha de uma release,
 * resolvendo o ref (release atual, versão mais recente ou tag).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsBuildService {

  private final ReleaseService releaseService;
  private final ReleaseRepository releaseRepository;
  private final ReleaseHistoricoRepository historicoRepository;
  private final GitHubReleasesAdapter github;
  private final LocalGitCatalog localGit;
  private final JenkinsAdapter jenkins;

  @Transactional(readOnly = true)
  public FontesBuildResponse listarFontes(UUID releaseId) {
    Release release = releaseService.buscar(releaseId);
    ProdutoRh produto = release.getProduto();
    String tagAtual = tagDaVersao(release.getVersao());
    FonteBuild atual = new FonteBuild(OrigemBuild.RELEASE_ATUAL.name(), tagAtual,
        normalizarVersao(release.getVersao()), null, null);

    boolean jenkinsOk = produto.temIntegracaoJenkins()
        && produto.getJenkinsToken() != null && !produto.getJenkinsToken().isBlank();
    String aviso = avisoJenkins(produto, jenkinsOk);

    FonteBuild ultima = null;
    List<TagBuild> tags = List.of();
    String githubErro = null;
    if (temOrigemGit(produto)) {
      try {
        GitSnapshot snap = carregarOrigemGit(produto);
        ultima = snap.ultimaGerada();
        tags = snap.tags();
      } catch (GitHubException e) {
        githubErro = e.getMessage();
        log.warn("Não listou tags/releases de {}: {}", produto.getRepositorioGithub(), e.getMessage());
      }
    }

    return new FontesBuildResponse(
        release.getId(),
        release.getVersao(),
        jenkinsOk,
        temOrigemGit(produto),
        produto.getJenkinsUrl(),
        produto.getJenkinsJob(),
        aviso,
        githubErro,
        atual,
        ultima,
        tags);
  }

  @Transactional
  public DispararBuildResponse disparar(UUID releaseId, DispararBuildRequest request) {
    return disparar(releaseId, request, null);
  }

  @Transactional
  public DispararBuildResponse disparar(UUID releaseId, DispararBuildRequest request, String jobOverride) {
    Release release = releaseService.buscar(releaseId);
    ProdutoRh produto = release.getProduto();
    OrigemBuild origem = request.origem();
    if (origem == null) {
      throw new BusinessException("Informe a origem do build (release atual, última gerada ou tag).");
    }
    if (release.getStatus() == ReleaseStatus.CANCELADA && origem == OrigemBuild.RELEASE_ATUAL) {
      throw new BusinessException("Não é possível gerar artefato de uma release cancelada.");
    }
    if (!produto.temIntegracaoJenkins()) {
      throw new BusinessException("Configure a URL e o job Jenkins no cadastro do produto.");
    }
    if (produto.getJenkinsToken() == null || produto.getJenkinsToken().isBlank()) {
      throw new BusinessException("Configure o API token Jenkins no cadastro do produto.");
    }
    String job = jobOverride != null && !jobOverride.isBlank() ? jobOverride.trim() : produto.getJenkinsJob();

    String tag = resolverTag(release, produto, origem, request.tag());
    String versao = versaoPortal(tag);
    String gitRef = gitRefDaTag(tag, produto.getBranchPadrao());

    Map<String, String> params = JenkinsAdapter.parametrosPadrao(
        produto.getSigla(), versao, tag, gitRef, origem.name());

    JenkinsQueueInfo queue;
    try {
      queue = jenkins.dispararBuild(
          produto.getJenkinsUrl(),
          job,
          produto.getJenkinsUser(),
          produto.getJenkinsToken(),
          params);
    } catch (JenkinsException e) {
      throw new BusinessException(e.getMessage());
    }

    Optional<Release> afetada = releaseRepository
        .findByProdutoSiglaAndVersao(produto.getSigla(), versao)
        .filter(r -> r.getStatus() != ReleaseStatus.CANCELADA);
    String aviso = null;
    UUID idAfetada = null;
    if (afetada.isPresent()) {
      Release alvo = afetada.get();
      alvo.atualizarBuildStatus("EM_ANDAMENTO", null,
          queue.queueUrl() != null ? queue.queueUrl() : produto.getJenkinsUrl());
      String descricao = "Build Jenkins disparado (" + origem.name() + ") na tag " + tag;
      historicoRepository.save(new ReleaseHistorico(
          alvo.getId(), AcaoHistorico.BUILD_DISPARADO, descricao, null, null, username()));
      idAfetada = alvo.getId();
      if (!alvo.getId().equals(release.getId())) {
        aviso = "O status do build será associado à release " + versao
            + " do portal, não a esta ficha.";
      }
    } else {
      aviso = "Não há release " + versao + " no portal — o Jenkins vai gerar o artefato, "
          + "mas o webhook só atualiza o status quando essa versão existir.";
    }

    log.info("Jenkins disparado: produto={} origem={} tag={} job={}",
        produto.getSigla(), origem, tag, job);

    return new DispararBuildResponse(
        origem.name(),
        tag,
        versao,
        release.getId(),
        idAfetada,
        job,
        queue.queueUrl(),
        idAfetada != null ? "EM_ANDAMENTO" : null,
        aviso,
        List.of(new DispararBuildResponse.JobEnfileirado(
            null, produto.getSigla(), job, tag, queue.queueUrl(), aviso)));
  }

  private String resolverTag(Release release, ProdutoRh produto, OrigemBuild origem, String tagInformada) {
    return switch (origem) {
      case RELEASE_ATUAL -> tagDaVersao(release.getVersao());
      case ULTIMA_GERADA -> {
        GitHubRelease ultima = buscarUltimaGerada(produto)
            .orElseThrow(() -> new BusinessException(
                "Não há GitHub Release publicada neste repositório."));
        yield ultima.tagName();
      }
      case TAG_ESPECIFICA -> {
        if (tagInformada == null || tagInformada.isBlank()) {
          throw new BusinessException("Informe a tag Git para gerar o artefato.");
        }
        String tag = tagDaVersao(tagInformada.trim());
        if (!tagOuBranchPermitida(produto, tag)) {
          throw new BusinessException(
              "Tag '" + tag + "' não combina com o padrão do produto ("
                  + produto.getPadraoTag() + ").");
        }
        yield tag;
      }
    };
  }

  private Optional<GitHubRelease> buscarUltimaGerada(ProdutoRh produto) {
    if (!temOrigemGit(produto)) {
      throw new BusinessException(
          "Configure o repositório Git no produto para usar a última release gerada.");
    }
    try {
      FonteBuild ultima = carregarOrigemGit(produto).ultimaGerada();
      if (ultima == null || ultima.tag() == null) return Optional.empty();
      return Optional.of(new GitHubRelease(
          ultima.tag(), ultima.versao(), false, false, ultima.publishedAt(), List.of()));
    } catch (GitHubException e) {
      throw new BusinessException(e.getMessage());
    }
  }

  boolean temOrigemGit(ProdutoRh produto) {
    String repo = produto.getRepositorioGithub();
    return repo != null && !repo.isBlank()
        && (produto.temIntegracaoGithub() || localGit.temClone(repo));
  }

  private GitSnapshot carregarOrigemGit(ProdutoRh produto) {
    if (produto.temIntegracaoGithub()) {
      return carregarGit(produto);
    }
    if (localGit.temClone(produto.getRepositorioGithub())) {
      return carregarGitLocal(produto);
    }
    return new GitSnapshot(null, List.of());
  }

  private GitSnapshot carregarGitLocal(ProdutoRh produto) {
    List<LocalGitRef> refs = localGit.listarRefs(produto.getRepositorioGithub());
    Map<String, TagBuild> tags = new LinkedHashMap<>();
    for (LocalGitRef r : refs) {
      if (!tagCombinaPadrao(produto, r.name())) continue;
      tags.putIfAbsent(r.name(), new TagBuild(
          r.name(),
          normalizarVersao(r.name()),
          !r.branch(),
          null));
    }
    FonteBuild ultima = tags.values().stream()
        .max(java.util.Comparator.comparing(TagBuild::tag, JenkinsBuildService::compararVersao))
        .map(t -> new FonteBuild(
            OrigemBuild.ULTIMA_GERADA.name(), t.tag(), t.versao(), t.totalAssets(), null))
        .orElse(null);
    return new GitSnapshot(ultima, new ArrayList<>(tags.values()));
  }

  private GitSnapshot carregarGit(ProdutoRh produto) {
    List<GitHubRelease> releases = github.listarReleases(
        produto.getRepositorioGithub(), produto.getGithubToken(), 40, true);
    List<GitHubTag> gitTags = new ArrayList<>();
    List<GitHubTag> tagsApi = github.listarTags(
        produto.getRepositorioGithub(), produto.getGithubToken(), 100);
    if (tagsApi != null) gitTags.addAll(tagsApi);
    for (String prefixo : prefixosRefsGit(produto.getPadraoTag())) {
      addMatchingRefs(gitTags, produto, prefixo);
    }

    Map<String, GitHubRelease> porTag = new LinkedHashMap<>();
    for (GitHubRelease r : releases) {
      if (r.tagName() == null || r.tagName().isBlank()) continue;
      porTag.putIfAbsent(r.tagName(), r);
    }

    Map<String, TagBuild> tags = new LinkedHashMap<>();
    for (GitHubRelease r : releases) {
      if (r.tagName() == null || r.tagName().isBlank()) continue;
      if (!tagCombinaPadrao(produto, r.tagName())) continue;
      tags.putIfAbsent(r.tagName(), new TagBuild(
          r.tagName(),
          normalizarVersao(r.tagName()),
          true,
          r.assets() == null ? 0 : r.assets().size()));
    }
    for (GitHubTag t : gitTags) {
      if (!tagCombinaPadrao(produto, t.name())) continue;
      tags.putIfAbsent(t.name(), new TagBuild(
          t.name(),
          normalizarVersao(t.name()),
          porTag.containsKey(t.name()),
          porTag.containsKey(t.name()) && porTag.get(t.name()).assets() != null
              ? porTag.get(t.name()).assets().size() : null));
    }
    FonteBuild ultima = tags.values().stream()
        .max(java.util.Comparator.comparing(TagBuild::tag, JenkinsBuildService::compararVersao))
        .map(t -> {
          GitHubRelease rel = porTag.get(t.tag());
          return new FonteBuild(
              OrigemBuild.ULTIMA_GERADA.name(),
              t.tag(),
              t.versao(),
              t.totalAssets(),
              rel == null ? null : rel.publishedAt());
        })
        .orElse(null);
    return new GitSnapshot(ultima, new ArrayList<>(tags.values()));
  }

  private void addMatchingRefs(List<GitHubTag> dest, ProdutoRh produto, String prefixo) {
    List<GitHubTag> extra = github.listarMatchingRefs(
        produto.getRepositorioGithub(), produto.getGithubToken(), prefixo);
    if (extra != null) dest.addAll(extra);
  }

  private String avisoJenkins(ProdutoRh produto, boolean jenkinsOk) {
    if (jenkinsOk) return null;
    if (!produto.temIntegracaoJenkins()) {
      return "Configure URL e job Jenkins no cadastro do produto para gerar o artefato.";
    }
    return "Configure o API token Jenkins no cadastro do produto.";
  }

  static String tagDaVersao(String versao) {
    String v = versao == null ? "" : versao.trim();
    if (v.isEmpty()) return v;
    if (v.startsWith("refs/") || v.startsWith("release/")
        || v.startsWith("v") || v.startsWith("V")) {
      return v;
    }
    return "v" + v;
  }

  /**
   * Prefixos GitHub Git refs API derivados do {@code padraoTag}
   * ({@code v?5} → tags/v5, tags/V5, heads/release/v5).
   */
  static List<String> prefixosRefsGit(String padraoTag) {
    String major = majorDoPadraoTag(padraoTag);
    if (major == null) {
      return List.of();
    }
    return List.of("tags/v" + major, "tags/V" + major, "heads/release/v" + major);
  }

  static String majorDoPadraoTag(String padrao) {
    if (padrao == null || padrao.isBlank()) {
      return null;
    }
    var m = Pattern.compile("v\\?(\\d+)").matcher(padrao);
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

  static String gitRefDaTag(String tag) {
    return gitRefDaTag(tag, null);
  }

  static String gitRefDaTag(String tag, String branchPadrao) {
    String t = tag == null ? "" : tag.trim();
    if (t.isEmpty()) return t;
    if (t.startsWith("refs/")) return t;
    if (branchPadrao != null && !branchPadrao.isBlank() && t.equals(branchPadrao.trim())) {
      return "refs/heads/" + t;
    }
    if (t.startsWith("release/")) return "refs/heads/" + t;
    return "refs/tags/" + tagDaVersao(t);
  }

  static String normalizarVersao(String tagOuVersao) {
    if (tagOuVersao == null) return "";
    String v = tagOuVersao.trim();
    if (v.startsWith("refs/tags/")) v = v.substring("refs/tags/".length());
    if (v.startsWith("refs/heads/")) v = v.substring("refs/heads/".length());
    if (v.startsWith("release/")) v = v.substring("release/".length());
    if (v.startsWith("v") || v.startsWith("V")) return v.substring(1);
    return v;
  }

  /**
   * Versão gravada no portal / enviada ao Jenkins. Branch tipo {@code v5/main}
   * vira {@code v5-main}; tags {@code v5.4.2} viram {@code 5.4.2}.
   */
  static String versaoPortal(String tagOuVersao) {
    if (tagOuVersao == null) return "";
    String v = tagOuVersao.trim();
    if (v.startsWith("refs/tags/")) v = v.substring("refs/tags/".length());
    else if (v.startsWith("refs/heads/")) v = v.substring("refs/heads/".length());
    if (v.startsWith("release/")) v = v.substring("release/".length());
    if (v.contains("/")) return v.replace('/', '-');
    if (v.startsWith("v") || v.startsWith("V")) return v.substring(1);
    return v;
  }

  static boolean tagCombinaPadrao(ProdutoRh produto, String tag) {
    String padrao = produto.getPadraoTag();
    if (padrao == null || padrao.isBlank() || tag == null) return true;
    try {
      return Pattern.compile(padrao).matcher(tag).matches();
    } catch (PatternSyntaxException e) {
      return true;
    }
  }

  static boolean tagOuBranchPermitida(ProdutoRh produto, String tag) {
    if (tag == null || tag.isBlank()) return false;
    String t = tag.trim();
    String branch = produto.getBranchPadrao();
    if (branch != null && !branch.isBlank() && t.equals(branch.trim())) {
      return true;
    }
    return tagCombinaPadrao(produto, t);
  }

  /**
   * Compara tags/branches pela versão numérica ({@code 5.4.3} &gt; {@code 5.3.58.3}).
   * Empate: tag Git ganha da branch {@code release/...}.
   */
  static int compararVersao(String tagA, String tagB) {
    int cmp = compararSegmentos(normalizarVersao(tagA), normalizarVersao(tagB));
    if (cmp != 0) return cmp;
    return Integer.compare(pesoRef(tagA), pesoRef(tagB));
  }

  private static int pesoRef(String tag) {
    String t = tag == null ? "" : tag.trim();
    if (t.startsWith("refs/heads/") || t.startsWith("release/")) return 0;
    return 1;
  }

  private static int compararSegmentos(String va, String vb) {
    int[] a = segmentosNumericos(va);
    int[] b = segmentosNumericos(vb);
    int n = Math.max(a.length, b.length);
    for (int i = 0; i < n; i++) {
      int x = i < a.length ? a[i] : 0;
      int y = i < b.length ? b[i] : 0;
      if (x != y) return Integer.compare(x, y);
    }
    return 0;
  }

  private static int[] segmentosNumericos(String versao) {
    if (versao == null || versao.isBlank()) return new int[0];
    String[] parts = versao.split("\\.");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      String digits = parts[i].replaceFirst("\\D.*", "");
      if (digits.isEmpty()) {
        out[i] = 0;
        continue;
      }
      try {
        out[i] = Integer.parseInt(digits);
      } catch (NumberFormatException e) {
        out[i] = 0;
      }
    }
    return out;
  }

  private String username() {
    var auth = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication();
    return auth == null ? "system" : auth.getName();
  }

  private record GitSnapshot(FonteBuild ultimaGerada, List<TagBuild> tags) {}
}
