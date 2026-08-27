package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.config.LinuxManualProperties;
import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ConfiguracaoInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.integration.ssh.HostSshClient;
import com.nexus.portal.releaseorchestrator.integration.ssh.LocalHostFsClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinuxManualImplantacaoAdapterTest {

  @Mock HostSshClient ssh;
  @Mock LocalHostFsClient localFs;
  @Mock LinuxManualEnvFactory envFactory;
  LinuxManualImplantacaoAdapter adapter;
  Host host;
  InstalacaoCliente instalacao;
  Release release;

  @BeforeEach
  void setUp() {
    adapter = new LinuxManualImplantacaoAdapter(
        ssh, localFs, envFactory, new LinuxManualProperties("/tmp/template", false));
    Cliente cliente = new Cliente("BMW", "BMW", AmbientePadrao.HOM);
    host = new Host("SRV", "Srv", "cli-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    host.setUsuarioConexao("dtec");
    host.setPortaConexao(22);
    ProdutoRh produto = new ProdutoRh("LD", "LD", null, "#fff", null, true);
    instalacao = new InstalacaoCliente("BMW-LD-01", "LD", cliente, host, produto,
        TipoImplantacao.LINUX_MANUAL, AmbientePadrao.HOM);
    instalacao.setDiretorioInstalacao("/opt/dtec/bmw");
    ConfiguracaoInstalacao cfg = new ConfiguracaoInstalacao(instalacao);
    cfg.atualizar(TipoBanco.SQLSERVER, "db", 1433, "BMW", "u", "p", null, null, null);
    instalacao.setConfiguracao(cfg);
    release = new Release(produto, "5.0.0", "V5", TipoRelease.MAJOR, ReleaseStatus.PUBLICADA,
        null, null, null, null);
  }

  @Test
  void aplicar_escreveEnvEChamaInstallComStart() {
    when(ssh.existe(host, "/opt/dtec/bmw/scripts/install.sh")).thenReturn(true);
    when(envFactory.gerar(instalacao)).thenReturn(pacote());
    when(ssh.exec(any(), any(), any())).thenReturn(new HostSshClient.Resultado(0, "ok", ""));

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.CRIAR, manifesto()));

    assertThat(r.ok()).isTrue();
    verify(ssh).enviar(eq(host), eq("/opt/dtec/bmw/.env"), any());
    verify(ssh).enviar(eq(host), eq("/opt/dtec/bmw/templates/context-oracle.xml.template"), any());
    verify(ssh).exec(eq(host), contains("scripts/install.sh --start"),
        eq(Duration.ofMinutes(20)));
    verify(ssh, never()).exec(any(), contains("scripts/stop.sh"), any());
    verify(localFs, never()).copiarArvore(any(), any());
  }

  @Test
  void aplicar_skipStartAindaRodaInstallSemStart() {
    instalacao.getConfiguracao().setParametros("SKIP_START=true");
    when(ssh.existe(host, "/opt/dtec/bmw/scripts/install.sh")).thenReturn(true);
    when(envFactory.gerar(instalacao)).thenReturn(pacote());
    when(ssh.exec(any(), any(), any())).thenReturn(new HostSshClient.Resultado(0, "ok", ""));

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.CRIAR, manifesto()));

    assertThat(r.ok()).isTrue();
    assertThat(r.mensagem()).contains("SKIP_START");
    verify(ssh).exec(eq(host), argThat(cmd -> cmd.contains("install.sh") && !cmd.contains("--start")),
        eq(Duration.ofMinutes(20)));
  }

  @Test
  void aplicar_atualizarParaAntesDeInstalar() {
    when(ssh.existe(host, "/opt/dtec/bmw/scripts/install.sh")).thenReturn(true);
    when(envFactory.gerar(instalacao)).thenReturn(pacote());
    when(ssh.exec(any(), any(), any())).thenReturn(new HostSshClient.Resultado(0, "ok", ""));

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.ATUALIZAR, manifesto()));

    assertThat(r.ok()).isTrue();
    verify(ssh).exec(eq(host), contains("scripts/stop.sh"), eq(Duration.ofSeconds(60)));
    verify(ssh).exec(eq(host), contains("scripts/install.sh --start"),
        eq(Duration.ofMinutes(20)));
  }

  @Test
  void aplicar_localhostCriaArvoreNaPrimeiraVez() {
    adapter = new LinuxManualImplantacaoAdapter(
        ssh, localFs, envFactory, new LinuxManualProperties("/tmp/template", true));
    host.setHostname("localhost");
    when(localFs.existe(host, "/opt/dtec/bmw/scripts/install.sh")).thenReturn(false);
    when(envFactory.gerar(instalacao)).thenReturn(pacote());
    when(localFs.exec(any(), any(), any())).thenReturn(new HostSshClient.Resultado(0, "ok", ""));

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.CRIAR, manifesto()));

    assertThat(r.ok()).isTrue();
    verify(localFs).copiarArvore(eq(Path.of("/tmp/template")), eq(Path.of("/opt/dtec/bmw")));
    verify(localFs).enviar(eq(host), eq("/opt/dtec/bmw/.env"), any());
    verify(localFs).exec(eq(host), contains("scripts/install.sh --start"),
        eq(Duration.ofMinutes(20)));
  }

  @Test
  void aplicar_iniciarRodaStartSh() {
    when(ssh.existe(host, "/opt/dtec/bmw/scripts/start.sh")).thenReturn(true);
    when(ssh.exec(any(), any(), any())).thenReturn(new HostSshClient.Resultado(0, "ok", ""));

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.INICIAR, manifesto()));

    assertThat(r.ok()).isTrue();
    assertThat(r.mensagem()).contains("start.sh");
    verify(ssh).exec(eq(host), contains("scripts/start.sh"), eq(Duration.ofMinutes(5)));
    verify(ssh, never()).exec(any(), contains("scripts/install.sh"), any());
    verify(envFactory, never()).gerar(any());
  }

  @Test
  void aplicar_pararRodaStopSh() {
    when(ssh.existe(host, "/opt/dtec/bmw/scripts/stop.sh")).thenReturn(true);
    when(ssh.exec(any(), any(), any())).thenReturn(new HostSshClient.Resultado(0, "ok", ""));

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.PARAR, manifesto()));

    assertThat(r.ok()).isTrue();
    verify(ssh).exec(eq(host), contains("scripts/stop.sh"), eq(Duration.ofSeconds(60)));
    verify(envFactory, never()).gerar(any());
  }

  @Test
  void aplicar_falhaSePastaNaoTemScriptsRemoto() {
    when(ssh.existe(host, "/opt/dtec/bmw/scripts/install.sh")).thenReturn(false);

    ImplantacaoAdapter.Resultado r = adapter.aplicar(new ImplantacaoAdapter.Contexto(
        release, instalacao, host, OperacaoDeploy.CRIAR, manifesto()));

    assertThat(r.ok()).isFalse();
    assertThat(r.erro()).contains("localhost");
  }

  private ManifestoImplantacaoResponse manifesto() {
    return new ManifestoImplantacaoResponse(
        UUID.randomUUID(), "5.0.0", TipoImplantacao.LINUX_MANUAL, null, null, "/opt/dtec/bmw",
        null, "linux", true, "fp");
  }

  private static LinuxManualEnvFactory.PacoteConfig pacote() {
    return new LinuxManualEnvFactory.PacoteConfig(
        "LEGACY_HTTP_PORT=8010\n", "<Context/>", "database=sqlserver",
        "./runtime/apache-tomcat-9.0.98", "sqlserver");
  }
}
