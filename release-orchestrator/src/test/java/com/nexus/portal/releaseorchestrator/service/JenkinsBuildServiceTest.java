package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.DispararBuildRequest;
import com.nexus.portal.releaseorchestrator.dto.request.OrigemBuild;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesBuildResponse;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.integration.git.LocalGitCatalog;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubTag;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsAdapter;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsQueueInfo;
import com.nexus.portal.releaseorchestrator.repository.ReleaseHistoricoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JenkinsBuildServiceTest {

  @Mock ReleaseService releaseService;
  @Mock ReleaseRepository releaseRepository;
  @Mock ReleaseHistoricoRepository historicoRepository;
  @Mock GitHubReleasesAdapter github;
  @Mock LocalGitCatalog localGit;
  @Mock JenkinsAdapter jenkins;
  @InjectMocks JenkinsBuildService service;

  UUID releaseId;
  ProdutoRh produto;
  Release release;

  @BeforeEach
  void setUp() {
    releaseId = UUID.randomUUID();
    produto = new ProdutoRh("Softon", "LD", null, "#111111", null, true);
    produto.setId(UUID.randomUUID());
    produto.atualizarIntegracaoGithub("org/ld", "main", "^v\\d+\\.\\d+\\.\\d+$", "ghp_x");
    produto.atualizarIntegracaoJenkins("http://localhost:8090", "ld-build", "admin", "token",
        "BUILD_ON_TAG");
    release = new Release(produto, "5.0.0", "V5", TipoRelease.MAJOR,
        ReleaseStatus.EM_DESENVOLVIMENTO, null, null, null, null);
    release.setId(releaseId);
    when(localGit.temClone(any())).thenReturn(false);
  }

  @Test
  void listarFontes_incluiReleaseAtualUltimaETags() {
    when(releaseService.buscar(releaseId)).thenReturn(release);
    GitHubRelease antiga = new GitHubRelease("v4.8.0", "4.8.0", false, false,
        OffsetDateTime.parse("2025-01-01T00:00:00Z"), List.of());
    GitHubRelease ultima = new GitHubRelease("v4.9.0", "4.9.0", false, false,
        OffsetDateTime.parse("2026-08-01T00:00:00Z"),
        List.of(new GitHubRelease.Asset(1L, "app.war", 10L, "x", "http://x")));
    when(github.listarReleases(eq("org/ld"), eq("ghp_x"), anyInt(), anyBoolean()))
        .thenReturn(List.of(antiga, ultima));
    when(github.listarTags(eq("org/ld"), eq("ghp_x"), anyInt()))
        .thenReturn(List.of(new GitHubTag("v5.0.0", "abc"), new GitHubTag("v4.9.0", "def")));

    FontesBuildResponse r = service.listarFontes(releaseId);

    assertThat(r.jenkinsConfigurado()).isTrue();
    assertThat(r.releaseAtual().tag()).isEqualTo("v5.0.0");
    assertThat(r.ultimaGerada().tag()).isEqualTo("v5.0.0");
    assertThat(r.tags()).extracting(FontesBuildResponse.TagBuild::tag)
        .contains("v5.0.0", "v4.9.0");
  }

  @Test
  void listarFontes_semJenkins_avisa() {
    produto.atualizarIntegracaoJenkins(null, null, null, "x", "BUILD_ON_TAG");
    when(releaseService.buscar(releaseId)).thenReturn(release);
    when(github.listarReleases(eq("org/ld"), eq("ghp_x"), anyInt(), anyBoolean()))
        .thenReturn(List.of());
    when(github.listarTags(eq("org/ld"), eq("ghp_x"), anyInt())).thenReturn(List.of());

    FontesBuildResponse r = service.listarFontes(releaseId);

    assertThat(r.jenkinsConfigurado()).isFalse();
    assertThat(r.aviso()).contains("Jenkins");
  }

  @Test
  void disparar_releaseAtual_enfileiraEMarcaEmAndamento() {
    when(releaseService.buscar(releaseId)).thenReturn(release);
    when(jenkins.dispararBuild(eq("http://localhost:8090"), eq("ld-build"), eq("admin"),
        eq("token"), anyMap())).thenReturn(new JenkinsQueueInfo("http://j/queue/item/9/"));
    when(releaseRepository.findByProdutoSiglaAndVersao("LD", "5.0.0"))
        .thenReturn(Optional.of(release));

    DispararBuildResponse r = service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.RELEASE_ATUAL, null));

    assertThat(r.tag()).isEqualTo("v5.0.0");
    assertThat(r.versao()).isEqualTo("5.0.0");
    assertThat(r.ultimoBuildStatus()).isEqualTo("EM_ANDAMENTO");
    assertThat(release.getUltimoBuildStatus()).isEqualTo("EM_ANDAMENTO");
    assertThat(r.queueUrl()).contains("/queue/item/9/");
    verify(historicoRepository).save(any());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<java.util.Map<String, String>> captor = ArgumentCaptor.forClass(java.util.Map.class);
    verify(jenkins).dispararBuild(any(), any(), any(), any(), captor.capture());
    assertThat(captor.getValue()).containsEntry("TAG_NAME", "v5.0.0")
        .containsEntry("GIT_REF", "refs/tags/v5.0.0")
        .containsEntry("RELEASE_VERSION", "5.0.0")
        .containsEntry("PORTAL_PRODUCT_SIGLA", "LD");
  }

  @Test
  void disparar_tagEspecifica_usaTagInformada() {
    when(releaseService.buscar(releaseId)).thenReturn(release);
    when(jenkins.dispararBuild(any(), any(), any(), any(), anyMap()))
        .thenReturn(new JenkinsQueueInfo("http://j/queue/item/1/"));
    when(releaseRepository.findByProdutoSiglaAndVersao("LD", "4.8.1"))
        .thenReturn(Optional.empty());

    DispararBuildResponse r = service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.TAG_ESPECIFICA, "4.8.1"));

    assertThat(r.tag()).isEqualTo("v4.8.1");
    assertThat(r.aviso()).contains("Não há release 4.8.1");
    assertThat(r.releaseIdAfetada()).isNull();
  }

  @Test
  void disparar_ultimaGerada_usaGithub() {
    when(releaseService.buscar(releaseId)).thenReturn(release);
    when(github.listarReleases(eq("org/ld"), eq("ghp_x"), anyInt(), anyBoolean()))
        .thenReturn(List.of(
            new GitHubRelease("v4.8.0", "4.8.0", false, false,
                OffsetDateTime.parse("2025-01-01T00:00:00Z"), List.of()),
            new GitHubRelease("v4.9.0", "4.9.0", false, false,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"), List.of())));
    when(jenkins.dispararBuild(any(), any(), any(), any(), anyMap()))
        .thenReturn(new JenkinsQueueInfo(null));
    when(releaseRepository.findByProdutoSiglaAndVersao("LD", "4.9.0"))
        .thenReturn(Optional.empty());

    DispararBuildResponse r = service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.ULTIMA_GERADA, null));

    assertThat(r.tag()).isEqualTo("v4.9.0");
    assertThat(r.origem()).isEqualTo("ULTIMA_GERADA");
  }

  @Test
  void disparar_cancelada_bloqueiaReleaseAtual() {
    release.alterarStatus(ReleaseStatus.CANCELADA);
    when(releaseService.buscar(releaseId)).thenReturn(release);

    assertThatThrownBy(() -> service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.RELEASE_ATUAL, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("cancelada");
  }

  @Test
  void disparar_tagEspecificaSemTag_falha() {
    when(releaseService.buscar(releaseId)).thenReturn(release);

    assertThatThrownBy(() -> service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.TAG_ESPECIFICA, "  ")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Informe a tag");
  }

  @Test
  void disparar_tagForaDoPadrao_falha() {
    when(releaseService.buscar(releaseId)).thenReturn(release);

    assertThatThrownBy(() -> service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.TAG_ESPECIFICA, "release-foo")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("padrão");
  }

  @Test
  void tagDaVersao_prefixaV() {
    assertThat(JenkinsBuildService.tagDaVersao("5.0.0")).isEqualTo("v5.0.0");
    assertThat(JenkinsBuildService.tagDaVersao("v5.0.0")).isEqualTo("v5.0.0");
    assertThat(JenkinsBuildService.normalizarVersao("v5.0.0")).isEqualTo("5.0.0");
    assertThat(JenkinsBuildService.normalizarVersao("refs/tags/v5.0.0")).isEqualTo("5.0.0");
    assertThat(JenkinsBuildService.tagDaVersao("release/v5.4.1")).isEqualTo("release/v5.4.1");
    assertThat(JenkinsBuildService.gitRefDaTag("v5.4.1")).isEqualTo("refs/tags/v5.4.1");
    assertThat(JenkinsBuildService.gitRefDaTag("release/v5.4.1")).isEqualTo("refs/heads/release/v5.4.1");
    assertThat(JenkinsBuildService.gitRefDaTag("v5/main", "v5/main")).isEqualTo("refs/heads/v5/main");
    assertThat(JenkinsBuildService.gitRefDaTag("v5/main")).isEqualTo("refs/tags/v5/main");
    assertThat(JenkinsBuildService.normalizarVersao("release/v5.4.1")).isEqualTo("5.4.1");
    assertThat(JenkinsBuildService.versaoPortal("v5/main")).isEqualTo("v5-main");
    assertThat(JenkinsBuildService.versaoPortal("v5.4.2")).isEqualTo("5.4.2");
    assertThat(JenkinsBuildService.versaoPortal("release/v5.4.1")).isEqualTo("5.4.1");
  }

  @Test
  void tagOuBranchPermitida_aceitaBranchPadraoForaDoPadrao() {
    produto.atualizarIntegracaoGithub("org/ld", "v5/main", "^v\\d+\\.\\d+\\.\\d+$", "ghp_x");
    assertThat(JenkinsBuildService.tagOuBranchPermitida(produto, "v5/main")).isTrue();
    assertThat(JenkinsBuildService.tagOuBranchPermitida(produto, "v5.4.2")).isTrue();
    assertThat(JenkinsBuildService.tagOuBranchPermitida(produto, "release-foo")).isFalse();
  }

  @Test
  void tagCombinaPadrao_aceitaTagV5DoFamiliaSofton() {
    produto.atualizarIntegracaoGithub("softonsi/dtec-risco", "main-coso",
        "(?i)^(release/)?v?2(\\.[0-9]+)+$", "ghp_x");
    assertThat(JenkinsBuildService.tagCombinaPadrao(produto, "v2.3.18")).isTrue();
    assertThat(JenkinsBuildService.tagCombinaPadrao(produto, "release/v2.2.31")).isTrue();
    assertThat(JenkinsBuildService.tagCombinaPadrao(produto, "v1.3.14")).isFalse();
    assertThat(JenkinsBuildService.tagOuBranchPermitida(produto, "main-coso")).isTrue();
    assertThat(JenkinsBuildService.gitRefDaTag("main-coso", "main-coso"))
        .isEqualTo("refs/heads/main-coso");
  }

  @Test
  void prefixosRefsGit_derivaDoMajorDoPadrao() {
    assertThat(JenkinsBuildService.prefixosRefsGit("(?i)^(release/)?v?5(\\.[0-9]+)+$"))
        .containsExactly("tags/v5", "tags/V5", "heads/release/v5");
    assertThat(JenkinsBuildService.prefixosRefsGit("(?i)^(release/)?v?2(\\.[0-9]+)+$"))
        .containsExactly("tags/v2", "tags/V2", "heads/release/v2");
  }

  @Test
  void compararVersao_numeroGanhaDeDataDePublicacao() {
    assertThat(JenkinsBuildService.compararVersao("V5.4.3", "V5.3.58.3")).isPositive();
    assertThat(JenkinsBuildService.compararVersao("v5.4.2.1", "v5.4.2")).isPositive();
    assertThat(JenkinsBuildService.compararVersao("V5.4.3", "release/v5.4.3")).isPositive();
  }

  @Test
  void listarFontes_ultimaGerada_eMaiorVersaoMesmoSemGithubRelease() {
    produto.atualizarIntegracaoGithub("org/ld", "v5/main",
        "(?i)^(release/)?v?5(\\.[0-9]+)+$", "ghp_x");
    when(releaseService.buscar(releaseId)).thenReturn(release);
    when(github.listarReleases(eq("org/ld"), eq("ghp_x"), anyInt(), anyBoolean()))
        .thenReturn(List.of(new GitHubRelease("V5.3.58.3", "V5.3.58.3", false, false,
            OffsetDateTime.parse("2026-08-10T19:30:47Z"), List.of())));
    when(github.listarTags(eq("org/ld"), eq("ghp_x"), anyInt())).thenReturn(List.of());
    when(github.listarMatchingRefs(eq("org/ld"), eq("ghp_x"), eq("tags/V5")))
        .thenReturn(List.of(new GitHubTag("V5.4.3", "abc")));
    when(github.listarMatchingRefs(eq("org/ld"), eq("ghp_x"), eq("heads/release/v5")))
        .thenReturn(List.of(new GitHubTag("release/v5.4.3", "def")));

    FontesBuildResponse r = service.listarFontes(releaseId);

    assertThat(r.ultimaGerada().tag()).isEqualTo("V5.4.3");
  }

  @Test
  void disparar_jobOverride_usaJobInformado() {
    when(releaseService.buscar(releaseId)).thenReturn(release);
    when(jenkins.dispararBuild(any(), eq("cr-build"), any(), any(), anyMap()))
        .thenReturn(new JenkinsQueueInfo("http://j/queue/item/2/"));
    when(releaseRepository.findByProdutoSiglaAndVersao("LD", "5.0.0"))
        .thenReturn(Optional.of(release));

    DispararBuildResponse r = service.disparar(releaseId,
        new DispararBuildRequest(OrigemBuild.RELEASE_ATUAL, null), "cr-build");

    assertThat(r.jenkinsJob()).isEqualTo("cr-build");
    verify(jenkins).dispararBuild(any(), eq("cr-build"), any(), any(), anyMap());
  }
}
