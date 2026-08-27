package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.releaseorchestrator.dto.request.ExecutarDeployRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.DeployInstalacao;
import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.StatusDeploy;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorDeployInstalacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeployInstalacaoServiceTest {

  @Mock OrchestratorDeployInstalacaoRepository repository;
  @Mock OrchestratorInstalacaoClienteRepository instalacaoRepository;
  @Mock ReleaseRepository releaseRepository;
  @Mock OrchestratorEntregaRepository entregaRepository;
  @Mock ManifestoImplantacaoService manifestoService;
  @Mock ImplantacaoAdapterResolver adapters;
  @Mock ImplantacaoAdapter adapter;
  @Mock EscopoResolver escopoResolver;
  @InjectMocks DeployInstalacaoService service;

  UUID clienteId;
  UUID hostId;
  UUID produtoId;
  UUID releaseId;
  UUID instalacaoId;
  Cliente cliente;
  Host host;
  ProdutoRh produto;
  Release release;
  InstalacaoCliente instalacao;

  @BeforeEach
  void setUp() throws Exception {
    clienteId = UUID.randomUUID();
    hostId = UUID.randomUUID();
    produtoId = UUID.randomUUID();
    releaseId = UUID.randomUUID();
    instalacaoId = UUID.randomUUID();
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, clienteId);
    host = new Host("SRV-LIN-01", "Linux", "lin-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    host.setDockerDisponivel(true);
    host.setPortaConexao(2375);
    setId(host, hostId);
    produto = new ProdutoRh("RPA", "RPA", "Robô", "#2563eb", null, true);
    setId(produto, produtoId);
    release = new Release(produto, "1.5.0", "Junho", TipoRelease.MINOR, ReleaseStatus.PUBLICADA,
        null, null, null, null);
    setId(release, releaseId);
    instalacao = new InstalacaoCliente("ACME-RPA-01", "RPA ACME", cliente, host, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    setId(instalacao, instalacaoId);
    when(instalacaoRepository.findById(instalacaoId)).thenReturn(Optional.of(instalacao));
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(escopoResolver.podeAcessarCliente(any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(any());
    ManifestoImplantacaoResponse manifesto = new ManifestoImplantacaoResponse(
        releaseId, "1.5.0", TipoImplantacao.DOCKER_PULL, "rpa:1.5.0", null, null,
        null, "Docker pull rpa:1.5.0 (versão 1.5.0)", true, "fp-1");
    when(manifestoService.resolver(release, instalacao)).thenReturn(manifesto);
    when(repository.save(any(DeployInstalacao.class))).thenAnswer(inv -> inv.getArgument(0));
    when(adapters.resolver(any(), any())).thenReturn(adapter);
    when(adapter.aplicar(any())).thenReturn(ImplantacaoAdapter.Resultado.ok("Dry-run: ok"));
  }

  @Test
  void executar_dryRunConcluiSemAlterarHost() {
    DeployInstalacao d = service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, null, null));

    assertThat(d.getStatus()).isEqualTo(StatusDeploy.CONCLUIDO);
    assertThat(d.getOperacao()).isEqualTo(OperacaoDeploy.CRIAR);
    assertThat(d.getModo()).isEqualTo(ModoDeploy.DRY_RUN);
    assertThat(d.getMensagem()).contains("Dry-run");
    assertThat(instalacao.getStatus()).isEqualTo(StatusInstalacao.INEXISTENTE);
    assertThat(instalacao.getVersaoAtual()).isNull();
  }

  @Test
  void executar_realAtualizaInventario() {
    when(adapter.aplicar(any())).thenReturn(ImplantacaoAdapter.Resultado.ok("REAL: criou o container"));

    DeployInstalacao d = service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, null, ModoDeploy.REAL));

    assertThat(d.getStatus()).isEqualTo(StatusDeploy.CONCLUIDO);
    assertThat(d.getModo()).isEqualTo(ModoDeploy.REAL);
    assertThat(instalacao.getStatus()).isEqualTo(StatusInstalacao.ATIVA);
    assertThat(instalacao.getVersaoAtual()).isEqualTo("1.5.0");
    assertThat(instalacao.getHealth()).isEqualTo(HealthInstalacao.SAUDAVEL);
  }

  @Test
  void executar_realSemDockerMarcaFalha() {
    host.setDockerDisponivel(false);

    DeployInstalacao d = service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, null, ModoDeploy.REAL));

    assertThat(d.getStatus()).isEqualTo(StatusDeploy.FALHA);
    assertThat(d.getErro()).contains("Docker disponível");
    assertThat(instalacao.getHealth()).isEqualTo(HealthInstalacao.INDISPONIVEL);
    assertThat(instalacao.getStatus()).isEqualTo(StatusInstalacao.INEXISTENTE);
    verify(adapter, never()).aplicar(any());
  }

  @Test
  void executar_realRejeitaWindowsManual() {
    instalacao.atualizar("ACME-RPA-01", "RPA ACME", cliente, host, produto,
        TipoImplantacao.WINDOWS_MANUAL, StatusInstalacao.INEXISTENTE, AmbientePadrao.PROD,
        null, null, "C:\\Nexus\\RPA", null);

    assertThatThrownBy(() -> service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, null, ModoDeploy.REAL)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Linux manual");
  }

  @Test
  void executar_idempotenteNaoChamaAdaptadorDeNovo() throws Exception {
    DeployInstalacao anterior = new DeployInstalacao(
        release, instalacao, null, OperacaoDeploy.CRIAR, null, "1.5.0",
        "rpa:1.5.0", null, null, "fp-1", "admin");
    anterior.marcarConcluido("já foi");
    setId(anterior, UUID.randomUUID());
    when(repository.findFirstByRelease_IdAndInstalacao_IdAndFingerprintAndModoAndStatusOrderByCreatedAtDesc(
        eq(releaseId), eq(instalacaoId), eq("fp-1"), eq(ModoDeploy.DRY_RUN), eq(StatusDeploy.CONCLUIDO)))
        .thenReturn(Optional.of(anterior));

    DeployInstalacao d = service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, false, ModoDeploy.DRY_RUN));

    assertThat(d.getStatus()).isEqualTo(StatusDeploy.IGNORADO);
    assertThat(d.isReutilizado()).isTrue();
    assertThat(d.getMensagem()).contains("Idempotente");
    verify(adapter, never()).aplicar(any());
  }

  @Test
  void executarCicloVida_iniciarAtualizaHealthSemMudarVersao() throws Exception {
    instalacao.alterarStatus(StatusInstalacao.ATIVA);
    instalacao.definirVersaoAtual("1.5.0");
    DeployInstalacao anterior = new DeployInstalacao(
        release, instalacao, null, OperacaoDeploy.CRIAR, null, "1.5.0",
        "rpa:1.5.0", null, null, "fp-1", "admin");
    anterior.marcarConcluido("ok");
    setId(anterior, UUID.randomUUID());
    when(repository.findFirstByInstalacao_IdAndOperacaoInAndStatusOrderByCreatedAtDesc(
        eq(instalacaoId), any(), eq(StatusDeploy.CONCLUIDO)))
        .thenReturn(Optional.of(anterior));
    when(adapter.aplicar(any())).thenReturn(ImplantacaoAdapter.Resultado.ok("REAL: iniciou"));

    DeployInstalacao d = service.executarCicloVida(instalacaoId, OperacaoDeploy.INICIAR, ModoDeploy.REAL);

    assertThat(d.getOperacao()).isEqualTo(OperacaoDeploy.INICIAR);
    assertThat(d.getStatus()).isEqualTo(StatusDeploy.CONCLUIDO);
    assertThat(instalacao.getVersaoAtual()).isEqualTo("1.5.0");
    assertThat(instalacao.getHealth()).isEqualTo(HealthInstalacao.SAUDAVEL);
  }

  @Test
  void executarCicloVida_pararMarcaIndisponivel() throws Exception {
    instalacao.alterarStatus(StatusInstalacao.ATIVA);
    instalacao.definirVersaoAtual("1.5.0");
    DeployInstalacao anterior = new DeployInstalacao(
        release, instalacao, null, OperacaoDeploy.CRIAR, null, "1.5.0",
        "rpa:1.5.0", null, null, "fp-1", "admin");
    anterior.marcarConcluido("ok");
    setId(anterior, UUID.randomUUID());
    when(repository.findFirstByInstalacao_IdAndOperacaoInAndStatusOrderByCreatedAtDesc(
        eq(instalacaoId), any(), eq(StatusDeploy.CONCLUIDO)))
        .thenReturn(Optional.of(anterior));
    when(adapter.aplicar(any())).thenReturn(ImplantacaoAdapter.Resultado.ok("REAL: parou"));

    DeployInstalacao d = service.executarCicloVida(instalacaoId, OperacaoDeploy.PARAR, ModoDeploy.REAL);

    assertThat(d.getOperacao()).isEqualTo(OperacaoDeploy.PARAR);
    assertThat(d.getStatus()).isEqualTo(StatusDeploy.CONCLUIDO);
    assertThat(instalacao.getHealth()).isEqualTo(HealthInstalacao.INDISPONIVEL);
    assertThat(instalacao.getVersaoAtual()).isEqualTo("1.5.0");
  }

  @Test
  void executarCicloVida_rejeitaInexistente() {
    assertThatThrownBy(() -> service.executarCicloVida(
        instalacaoId, OperacaoDeploy.INICIAR, ModoDeploy.REAL))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Crie a instalação");
  }

  @Test
  void executar_rejeitaReleaseRascunho() {
    release.alterarStatus(ReleaseStatus.RASCUNHO);

    assertThatThrownBy(() -> service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("desenvolvimento");
  }

  @Test
  void executar_rejeitaInstalacaoInativa() {
    instalacao.alterarStatus(StatusInstalacao.INATIVA);

    assertThatThrownBy(() -> service.executar(
        new ExecutarDeployRequest(releaseId, instalacaoId, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("inativa");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
