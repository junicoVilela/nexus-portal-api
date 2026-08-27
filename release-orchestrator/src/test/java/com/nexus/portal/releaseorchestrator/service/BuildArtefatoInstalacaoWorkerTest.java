package com.nexus.portal.releaseorchestrator.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.config.LinuxManualProperties;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.BuildInstalacaoArtefato;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.StatusBuildInstalacao;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsAdapter;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsArtifact;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsBuildInfo;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsQueueItem;
import com.nexus.portal.releaseorchestrator.integration.ssh.HostSshClient;
import com.nexus.portal.releaseorchestrator.integration.ssh.LocalHostFsClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuildArtefatoInstalacaoWorkerTest {

  @Mock BuildAlvoInstalacaoService alvos;
  @Mock JenkinsAdapter jenkins;
  @Mock LocalHostFsClient localFs;
  @Mock HostSshClient ssh;

  BuildArtefatoInstalacaoWorker worker;
  ProdutoRh produto;
  Host host;
  InstalacaoCliente inst;
  BuildInstalacaoArtefato row;

  @BeforeEach
  void setUp() {
    worker = new BuildArtefatoInstalacaoWorker(
        alvos, jenkins, duration -> {}, localFs, ssh, new LinuxManualProperties("/tmp", true));
    produto = new ProdutoRh("LD", "LD", null, "#111", null, true);
    produto.setId(UUID.randomUUID());
    produto.atualizarIntegracaoJenkins("http://j", "ld-v5-build", "admin", "token", "BUILD_ON_TAG");
    Cliente cliente = new Cliente("BMW", "BMW", AmbientePadrao.HOM);
    cliente.setId(UUID.randomUUID());
    host = new Host("LOCAL", "Local", "localhost", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    inst = new InstalacaoCliente("BMW", "BMW - V5", cliente, host, produto,
        TipoImplantacao.LINUX_MANUAL, AmbientePadrao.HOM);
    inst.setId(UUID.randomUUID());
    inst.atualizar("BMW", "BMW - V5", cliente, host, produto, TipoImplantacao.LINUX_MANUAL,
        StatusInstalacao.INEXISTENTE, AmbientePadrao.HOM, null, null, "/opt/bmw", null);
    row = new BuildInstalacaoArtefato(inst, produto, null, "produto:" + produto.getId(),
        "ld-v5-build", "http://j/queue/item/9/", "*.war,*.jar,*.ear", null);
    row.setId(UUID.randomUUID());
  }

  @Test
  void acompanhar_copiaWarsEJarParaArtifacts() throws Exception {
    when(alvos.carregarParaAcompanhar(any())).thenReturn(List.of(row));
    when(jenkins.consultarFila(any(), any(), any(), any()))
        .thenReturn(Optional.of(new JenkinsQueueItem(false, 42, "http://j/job/ld-v5-build/42/")));
    when(jenkins.consultarBuild(any(), eq("ld-v5-build"), any(), any(), eq(42)))
        .thenReturn(Optional.of(new JenkinsBuildInfo(42, "SUCCESS", false, 0, 0, "http://j/42")));
    when(jenkins.listarArtefatos(any(), eq("ld-v5-build"), any(), any(), eq(42)))
        .thenReturn(List.of(
            new JenkinsArtifact("ldv4.war", "ldv4.war"),
            new JenkinsArtifact("ldv4-frontend.war", "ldv4-frontend.war"),
            new JenkinsArtifact("app.jar", "app.jar"),
            new JenkinsArtifact("app-plain.jar", "app-plain.jar")));
    when(jenkins.baixarArtefato(any(), any(), any(), any(), anyInt(), any()))
        .thenReturn("bin".getBytes());

    worker.acompanhar(List.of(row.getId()));

    verify(localFs).enviar(eq(host), eq("/opt/bmw/artifacts/ldv4.war"), any());
    verify(localFs).enviar(eq(host), eq("/opt/bmw/artifacts/ldv4-frontend.war"), any());
    verify(localFs).enviar(eq(host), eq("/opt/bmw/artifacts/app.jar"), any());
    verify(localFs, never()).enviar(eq(host), eq("/opt/bmw/artifacts/app-plain.jar"), any());
    verify(alvos).atualizarAcompanhamento(eq(row.getId()), eq(42), eq(StatusBuildInstalacao.SUCCESS), any());
    verify(ssh, never()).enviar(any(), any(), any());
  }

  @Test
  void acompanhar_semArtefatoMarcaFalha() {
    when(alvos.carregarParaAcompanhar(any())).thenReturn(List.of(row));
    when(jenkins.consultarFila(any(), any(), any(), any()))
        .thenReturn(Optional.of(new JenkinsQueueItem(false, 7, null)));
    when(jenkins.consultarBuild(any(), any(), any(), any(), eq(7)))
        .thenReturn(Optional.of(new JenkinsBuildInfo(7, "SUCCESS", false, 0, 0, null)));
    when(jenkins.listarArtefatos(any(), any(), any(), any(), eq(7))).thenReturn(List.of());

    worker.acompanhar(List.of(row.getId()));

    verify(alvos).atualizarAcompanhamento(eq(row.getId()), eq(7), eq(StatusBuildInstalacao.FAILED), any());
    verify(localFs, never()).enviar(any(), any(), any());
  }
}
