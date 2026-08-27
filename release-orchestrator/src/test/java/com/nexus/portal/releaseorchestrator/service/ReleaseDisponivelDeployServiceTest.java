package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.response.ReleaseDisponivelDeployResponse;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReleaseDisponivelDeployServiceTest {

  @Mock ReleaseRepository releaseRepository;
  @Mock ProdutoRhService produtoService;
  @Mock GitHubReleasesAdapter gitHub;
  @InjectMocks ReleaseDisponivelDeployService service;
  ProdutoRh produto;
  UUID produtoId;

  @BeforeEach
  void setUp() {
    produtoId = UUID.randomUUID();
    produto = new ProdutoRh("LD", "LD", null, "#fff", null, true);
    produto.atualizarIntegracaoGithub("org/ld", "main", "v*", "token");
  }

  @Test
  void listar_cruzaPortalComGitEIncluiEmAndamento() {
    when(produtoService.buscar(produtoId)).thenReturn(produto);
    Release andamento = new Release(produto, "5.1.0", "Andamento", TipoRelease.MINOR,
        ReleaseStatus.EM_DESENVOLVIMENTO, null, null, null, null);
    Release publicada = new Release(produto, "5.0.0", "Estável", TipoRelease.MAJOR,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    when(releaseRepository.findAll(ArgumentMatchers.<Specification<Release>>any(), any(Sort.class)))
        .thenReturn(List.of(andamento, publicada));
    when(gitHub.listarReleases(eq("org/ld"), eq("token"), anyInt(), anyBoolean()))
        .thenReturn(List.of(
            new GitHubRelease("v5.0.0", "5.0.0", false, false, OffsetDateTime.now(), List.of()),
            new GitHubRelease("v4.9.0", "4.9.0", false, false, OffsetDateTime.now(), List.of())));

    List<ReleaseDisponivelDeployResponse> r = service.listar(produtoId);

    assertThat(r).extracting(ReleaseDisponivelDeployResponse::versao)
        .contains("5.1.0", "5.0.0", "v4.9.0");
    ReleaseDisponivelDeployResponse pub = r.stream().filter(x -> x.versao().equals("5.0.0")).findFirst().orElseThrow();
    assertThat(pub.selecionavel()).isTrue();
    assertThat(pub.noGit()).isTrue();
    ReleaseDisponivelDeployResponse dev = r.stream().filter(x -> x.versao().equals("5.1.0")).findFirst().orElseThrow();
    assertThat(dev.emAndamento()).isTrue();
    ReleaseDisponivelDeployResponse soGit = r.stream().filter(x -> "v4.9.0".equals(x.versao())).findFirst().orElseThrow();
    assertThat(soGit.selecionavel()).isFalse();
    assertThat(soGit.noGit()).isTrue();
  }

  @Test
  void normalizar_removeV() {
    assertThat(ReleaseDisponivelDeployService.normalizar("v5.0.0")).isEqualTo("5.0.0");
    assertThat(ReleaseDisponivelDeployService.normalizar("5.0.0")).isEqualTo("5.0.0");
  }
}
