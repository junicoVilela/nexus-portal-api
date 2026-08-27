package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.releaseorchestrator.dto.request.DispararBuildRequest;
import com.nexus.portal.releaseorchestrator.dto.request.OrigemBuild;
import com.nexus.portal.releaseorchestrator.dto.request.ResolverVersaoInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ResolverVersaoInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.integration.git.LocalGitCatalog;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubTag;
import com.nexus.portal.releaseorchestrator.repository.BuildInstalacaoArtefatoRepository;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
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
class VersaoInstalacaoServiceTest {

  @Mock OrchestratorInstalacaoClienteRepository instalacaoRepository;
  @Mock ReleaseRepository releaseRepository;
  @Mock ReleaseHistoricoRepository historicoRepository;
  @Mock GitHubReleasesAdapter github;
  @Mock LocalGitCatalog localGit;
  @Mock JenkinsBuildService jenkinsBuildService;
  @Mock BuildAlvoInstalacaoService alvoService;
  @Mock BuildArtefatoInstalacaoWorker buildWorker;
  @Mock BuildInstalacaoArtefatoRepository buildArtefatoRepository;
  @Mock ModuloProdutoRepository moduloRepository;
  @Mock ProdutoRhRepository produtoRepository;
  @Mock EscopoResolver escopoResolver;
  @InjectMocks VersaoInstalacaoService service;

  UUID instalacaoId;
  UUID produtoId;
  ProdutoRh produto;
  InstalacaoCliente inst;

  @BeforeEach
  void setUp() {
    instalacaoId = UUID.randomUUID();
    produtoId = UUID.randomUUID();
    produto = new ProdutoRh("LD", "LD", null, "#111", null, true);
    produto.setId(produtoId);
    produto.atualizarIntegracaoGithub("org/ld", "v5/main", "^v\\d+\\.\\d+\\.\\d+$", "ghp_x");
    produto.atualizarIntegracaoJenkins("http://j", "ld-build", "admin", "token", "BUILD_ON_TAG");
    Cliente cliente = new Cliente("ACME", "ACME", AmbientePadrao.HOM);
    cliente.setId(UUID.randomUUID());
    Host host = new Host("LOCAL", "Local", "localhost", SistemaOperacionalHost.LINUX,
        TipoConexaoHost.SSH);
    inst = new InstalacaoCliente("ACME-LD", "Lab", cliente, host, produto,
        TipoImplantacao.LINUX_MANUAL, AmbientePadrao.HOM);
    inst.setId(instalacaoId);
    inst.definirVersaoAtual("5.0.0");
    when(instalacaoRepository.findById(instalacaoId)).thenReturn(Optional.of(inst));
    when(escopoResolver.podeAcessarCliente(any())).thenReturn(true);
    when(jenkinsBuildService.temOrigemGit(any())).thenReturn(true);
    when(alvoService.listar(any())).thenReturn(List.of());
    when(alvoService.selecionar(any(), any(), any())).thenReturn(List.of());
    when(alvoService.recentes(any())).thenReturn(List.of());
    when(localGit.temClone(any())).thenReturn(false);
    when(releaseRepository.save(any(Release.class))).thenAnswer(inv -> {
      Release r = inv.getArgument(0);
      if (r.getId() == null) {
        r.setId(UUID.randomUUID());
      }
      return r;
    });
  }

  @Test
  void listarFontes_trazVersaoAtualUltimaETags() {
    when(github.listarReleases(eq("org/ld"), eq("ghp_x"), anyInt(), anyBoolean()))
        .thenReturn(List.of(
            new GitHubRelease("v4.8.0", "4.8.0", false, false,
                OffsetDateTime.parse("2025-01-01T00:00:00Z"), List.of()),
            new GitHubRelease("v4.9.0", "4.9.0", false, false,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"), List.of())));
    when(github.listarTags(eq("org/ld"), eq("ghp_x"), anyInt()))
        .thenReturn(List.of(new GitHubTag("v5.0.0", "a"), new GitHubTag("v4.9.0", "b")));
    when(releaseRepository.findFirstByProduto_IdAndVersao(eq(produtoId), any()))
        .thenReturn(Optional.empty());

    FontesVersaoInstalacaoResponse r = service.listarFontes(instalacaoId);

    assertThat(r.versaoInstalada()).isEqualTo("5.0.0");
    assertThat(r.versaoAtual().tag()).isEqualTo("v5/main");
    assertThat(r.versaoAtual().versao()).isEqualTo("v5-main");
    assertThat(r.versaoAtual().selecionavel()).isTrue();
    assertThat(r.ultimaGerada().tag()).isEqualTo("v5.0.0");
    assertThat(r.tags()).extracting(FontesVersaoInstalacaoResponse.OpcaoVersao::tag)
        .contains("v5.0.0", "v4.9.0")
        .doesNotContain("v5/main");
    assertThat(r.jenkinsConfigurado()).isTrue();
  }

  @Test
  void listarFontes_ultimaGerada_eMaiorVersaoMesmoSemGithubRelease() {
    produto.atualizarIntegracaoGithub("org/ld", "v5/main",
        "(?i)^(release/)?v?5(\\.[0-9]+)+$", "ghp_x");
    when(github.listarReleases(eq("org/ld"), eq("ghp_x"), anyInt(), anyBoolean()))
        .thenReturn(List.of(new GitHubRelease("V5.3.58.3", "V5.3.58.3", false, false,
            OffsetDateTime.parse("2026-08-10T19:30:47Z"), List.of())));
    when(github.listarTags(eq("org/ld"), eq("ghp_x"), anyInt())).thenReturn(List.of());
    when(github.listarMatchingRefs(eq("org/ld"), eq("ghp_x"), eq("tags/V5")))
        .thenReturn(List.of(new GitHubTag("V5.4.3", "abc")));
    when(github.listarMatchingRefs(eq("org/ld"), eq("ghp_x"), eq("heads/release/v5")))
        .thenReturn(List.of(new GitHubTag("release/v5.4.3", "def")));
    when(releaseRepository.findFirstByProduto_IdAndVersao(eq(produtoId), any()))
        .thenReturn(Optional.empty());

    FontesVersaoInstalacaoResponse r = service.listarFontes(instalacaoId);

    assertThat(r.ultimaGerada().tag()).isEqualTo("V5.4.3");
  }

  @Test
  void resolver_criaReleaseQuandoSoExisteTag() {
    ResolverVersaoInstalacaoResponse r = service.resolver(instalacaoId,
        new ResolverVersaoInstalacaoRequest(OrigemBuild.TAG_ESPECIFICA, "v4.8.1"));

    assertThat(r.criada()).isTrue();
    assertThat(r.versao()).isEqualTo("4.8.1");
    assertThat(r.releaseId()).isNotNull();
    verify(historicoRepository).save(any());
  }

  @Test
  void resolver_versaoAtual_usaBranchPadrao() {
    when(releaseRepository.findFirstByProduto_IdAndVersao(produtoId, "v5-main"))
        .thenReturn(Optional.empty());

    ResolverVersaoInstalacaoResponse r = service.resolver(instalacaoId,
        new ResolverVersaoInstalacaoRequest(OrigemBuild.RELEASE_ATUAL, null));

    assertThat(r.tag()).isEqualTo("v5/main");
    assertThat(r.versao()).isEqualTo("v5-main");
    assertThat(r.criada()).isTrue();
  }

  @Test
  void resolver_versaoAtualSemBranch_falha() {
    produto.atualizarIntegracaoGithub("org/ld", "  ", "^v\\d+\\.\\d+\\.\\d+$", "ghp_x");

    assertThatThrownBy(() -> service.resolver(instalacaoId,
        new ResolverVersaoInstalacaoRequest(OrigemBuild.RELEASE_ATUAL, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("branch padrão");
  }

  @Test
  void resolver_reusaReleasePublicada() {
    Release pub = new Release(produto, "4.8.1", "Estável", TipoRelease.PATCH,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    pub.setId(UUID.randomUUID());
    when(releaseRepository.findFirstByProduto_IdAndVersao(produtoId, "4.8.1"))
        .thenReturn(Optional.of(pub));

    ResolverVersaoInstalacaoResponse r = service.resolver(instalacaoId,
        new ResolverVersaoInstalacaoRequest(OrigemBuild.TAG_ESPECIFICA, "4.8.1"));

    assertThat(r.criada()).isFalse();
    assertThat(r.releaseId()).isEqualTo(pub.getId());
  }

  @Test
  void dispararBuild_versaoAtual_enviaTagDaBranch() {
    when(jenkinsBuildService.disparar(any(), any())).thenReturn(
        new DispararBuildResponse("TAG_ESPECIFICA", "v5/main", "v5-main", UUID.randomUUID(),
            UUID.randomUUID(), "ld-build", "http://q", "EM_ANDAMENTO", null, List.of()));
    when(releaseRepository.findFirstByProduto_IdAndVersao(produtoId, "v5-main"))
        .thenReturn(Optional.empty());

    service.dispararBuild(instalacaoId,
        new DispararBuildRequest(OrigemBuild.RELEASE_ATUAL, null));

    ArgumentCaptor<DispararBuildRequest> captor = ArgumentCaptor.forClass(DispararBuildRequest.class);
    verify(jenkinsBuildService).disparar(any(), captor.capture());
    assertThat(captor.getValue().origem()).isEqualTo(OrigemBuild.TAG_ESPECIFICA);
    assertThat(captor.getValue().tag()).isEqualTo("v5/main");
  }

  @Test
  void dispararBuild_resolveDepoisDisparaJenkins() {
    when(jenkinsBuildService.disparar(any(), any())).thenReturn(
        new DispararBuildResponse("TAG_ESPECIFICA", "v4.8.1", "4.8.1", UUID.randomUUID(),
            UUID.randomUUID(), "ld-build", "http://q", "EM_ANDAMENTO", null, List.of()));

    DispararBuildResponse r = service.dispararBuild(instalacaoId,
        new DispararBuildRequest(OrigemBuild.TAG_ESPECIFICA, "v4.8.1"));

    assertThat(r.tag()).isEqualTo("v4.8.1");
    verify(jenkinsBuildService).disparar(any(), any());
  }

  @Test
  void dispararBuild_alvosDisparaJobEAgendaCopia() {
    FontesVersaoInstalacaoResponse.AlvoBuild alvo = new FontesVersaoInstalacaoResponse.AlvoBuild(
        "produto:" + produtoId, "ld", "Job LD", "PRODUTO", produtoId, "LD",
        "ld-v5-build", null, "*.war,*.jar", true, true);
    when(alvoService.selecionar(any(), any(), any())).thenReturn(List.of(alvo));
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(jenkinsBuildService.disparar(any(), any(), eq("ld-v5-build"))).thenReturn(
        new DispararBuildResponse("TAG_ESPECIFICA", "v5/main", "v5-main", UUID.randomUUID(),
            UUID.randomUUID(), "ld-v5-build", "http://j/queue/item/9/", "EM_ANDAMENTO", null,
            List.of()));
    when(buildArtefatoRepository.save(any())).thenAnswer(inv -> {
      var row = inv.getArgument(0, com.nexus.portal.releaseorchestrator.entity.BuildInstalacaoArtefato.class);
      row.setId(UUID.randomUUID());
      return row;
    });
    when(releaseRepository.findFirstByProduto_IdAndVersao(produtoId, "v5-main"))
        .thenReturn(Optional.empty());

    DispararBuildResponse r = service.dispararBuild(instalacaoId,
        new DispararBuildRequest(OrigemBuild.RELEASE_ATUAL, null, List.of(alvo.id())));

    assertThat(r.jobs()).hasSize(1);
    assertThat(r.jobs().get(0).jenkinsJob()).isEqualTo("ld-v5-build");
    verify(jenkinsBuildService).disparar(any(), any(), eq("ld-v5-build"));
    verify(buildWorker).acompanhar(any());
  }
}
