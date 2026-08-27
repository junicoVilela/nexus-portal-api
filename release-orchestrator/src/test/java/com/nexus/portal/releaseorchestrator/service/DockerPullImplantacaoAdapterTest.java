package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.integration.docker.DockerEngineClient;
import com.nexus.portal.releaseorchestrator.integration.docker.DockerEngineException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DockerPullImplantacaoAdapterTest {

  @Mock DockerEngineClient docker;
  DockerPullImplantacaoAdapter adapter;
  Host host;
  InstalacaoCliente instalacao;
  Release release;

  @BeforeEach
  void setUp() {
    adapter = new DockerPullImplantacaoAdapter(docker);
    Cliente cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    host = new Host("SRV", "Srv", "docker.acme.local", SistemaOperacionalHost.LINUX, TipoConexaoHost.DOCKER);
    host.setDockerDisponivel(true);
    host.setPortaConexao(2375);
    ProdutoRh produto = new ProdutoRh("RPA", "RPA", null, "#fff", null, true);
    instalacao = new InstalacaoCliente("ACME-RPA-01", "RPA", cliente, host, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    release = new Release(produto, "1.5.0", "Junho", TipoRelease.MINOR, ReleaseStatus.PUBLICADA,
        null, null, null, null);
  }

  @Test
  void aplicar_pullCriaEInicia() {
    ManifestoImplantacaoResponse manifesto = new ManifestoImplantacaoResponse(
        UUID.randomUUID(), "1.5.0", TipoImplantacao.DOCKER_PULL, "rpa:1.5.0", null, null,
        null, "pull", true, "fp");
    when(docker.criarContainer(any(), any(), any(), any(), any(), any())).thenReturn("abc");

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.CRIAR, manifesto));

    assertThat(r.ok()).isTrue();
    assertThat(r.mensagem()).contains("acme-rpa-01");
    verify(docker).ping("http://docker.acme.local:2375");
    verify(docker).pull("http://docker.acme.local:2375", "rpa", "1.5.0");
    verify(docker).removerSeExistir(eq("http://docker.acme.local:2375"), eq("acme-rpa-01"));
    verify(docker).iniciar(eq("http://docker.acme.local:2375"), eq("acme-rpa-01"));
  }

  @Test
  void baseUrl_rejeitaTls2376() {
    host.setPortaConexao(2376);
    assertThatThrownBy(() -> DockerPullImplantacaoAdapter.baseUrl(host))
        .isInstanceOf(DockerEngineException.class)
        .hasMessageContaining("2375");
  }

  @Test
  void aplicar_portaTlsDevolveFalha() {
    host.setPortaConexao(2376);
    ManifestoImplantacaoResponse manifesto = new ManifestoImplantacaoResponse(
        UUID.randomUUID(), "1.5.0", TipoImplantacao.DOCKER_PULL, "rpa:1.5.0", null, null,
        null, "pull", true, "fp");

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.CRIAR, manifesto));

    assertThat(r.ok()).isFalse();
    assertThat(r.erro()).contains("2375");
  }

  @Test
  void aplicar_iniciarNaoFazPull() {
    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.INICIAR, manifestoPull()));

    assertThat(r.ok()).isTrue();
    assertThat(r.mensagem()).contains("iniciou");
    verify(docker).ping("http://docker.acme.local:2375");
    verify(docker).iniciar("http://docker.acme.local:2375", "acme-rpa-01");
    verify(docker, never()).pull(any(), any(), any());
    verify(docker, never()).parar(any(), any());
  }

  @Test
  void aplicar_pararNaoFazPull() {
    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.PARAR, manifestoPull()));

    assertThat(r.ok()).isTrue();
    verify(docker).parar("http://docker.acme.local:2375", "acme-rpa-01");
    verify(docker, never()).iniciar(any(), any());
    verify(docker, never()).pull(any(), any(), any());
  }

  private ManifestoImplantacaoResponse manifestoPull() {
    return new ManifestoImplantacaoResponse(
        UUID.randomUUID(), "1.5.0", TipoImplantacao.DOCKER_PULL, "rpa:1.5.0", null, null,
        null, "pull", true, "fp");
  }

  @Test
  void parseImagem_respeitaRegistryComPorta() {
    var ref = DockerPullImplantacaoAdapter.ImageRef.parse("localhost:5000/rpa:1.5.0");
    assertThat(ref.imagem()).isEqualTo("localhost:5000/rpa");
    assertThat(ref.tag()).isEqualTo("1.5.0");
  }
}
