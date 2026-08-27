package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.InstalacaoClienteRequest;
import com.nexus.portal.releaseorchestrator.dto.request.RegistrarHealthInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorDeployInstalacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaInstalacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorHostRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
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
class InstalacaoClienteServiceTest {

  @Mock OrchestratorInstalacaoClienteRepository repository;
  @Mock OrchestratorClienteRepository clienteRepository;
  @Mock OrchestratorHostRepository hostRepository;
  @Mock ProdutoRhRepository produtoRepository;
  @Mock ReservaPortaService reservaPortaService;
  @Mock OrchestratorEntregaInstalacaoRepository entregaInstalacaoRepository;
  @Mock OrchestratorDeployInstalacaoRepository deployRepository;
  @Mock jakarta.persistence.EntityManager entityManager;
  @InjectMocks InstalacaoClienteService service;

  UUID clienteId;
  UUID hostId;
  UUID produtoId;
  Cliente cliente;
  Host hostLinuxDocker;
  ProdutoRh produto;

  @BeforeEach
  void setUp() throws Exception {
    clienteId = UUID.randomUUID();
    hostId = UUID.randomUUID();
    produtoId = UUID.randomUUID();
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, clienteId);
    hostLinuxDocker = new Host("SRV-LIN-01", "Linux 01", "srv-linux-01",
        SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    hostLinuxDocker.setDockerDisponivel(true);
    setId(hostLinuxDocker, hostId);
    produto = new ProdutoRh("RPA", "RPA", "Robô", "#2563eb", null, true);
    setId(produto, produtoId);
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
    when(hostRepository.findById(hostId)).thenReturn(Optional.of(hostLinuxDocker));
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(repository.save(any(InstalacaoCliente.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criar_dockerPullNormalizaCodigoEPermiteInexistenteSemImagem() {
    InstalacaoCliente inst = service.criar(req(
        "acme-rpa-01", "RPA ACME", TipoImplantacao.DOCKER_PULL, StatusInstalacao.INEXISTENTE,
        null, null, null));

    assertThat(inst.getCodigo()).isEqualTo("ACME-RPA-01");
    assertThat(inst.getTipoImplantacao()).isEqualTo(TipoImplantacao.DOCKER_PULL);
    assertThat(inst.getStatus()).isEqualTo(StatusInstalacao.INEXISTENTE);
    assertThat(inst.getImagemRef()).isNull();
  }

  @Test
  void criar_dockerPullAtivaExigeImagem() {
    assertThatThrownBy(() -> service.criar(req(
        "acme-rpa-01", "RPA ACME", TipoImplantacao.DOCKER_PULL, StatusInstalacao.ATIVA,
        null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("imagem");
  }

  @Test
  void criar_dockerTarAtivaExigeArquivo() {
    assertThatThrownBy(() -> service.criar(req(
        "acme-rpa-01", "RPA ACME", TipoImplantacao.DOCKER_TAR, StatusInstalacao.ATIVA,
        null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(".tar");
  }

  @Test
  void criar_dockerTarComArquivoPersiste() {
    InstalacaoCliente inst = service.criar(req(
        "acme-rpa-01", "RPA ACME", TipoImplantacao.DOCKER_TAR, StatusInstalacao.ATIVA,
        null, "rpa-1.4.0.tar", null));

    assertThat(inst.getArquivoImagemRef()).isEqualTo("rpa-1.4.0.tar");
  }

  @Test
  void criar_linuxManualExigeHostLinux() throws Exception {
    Host windows = new Host("SRV-WIN-01", "Win", "win-01",
        SistemaOperacionalHost.WINDOWS, TipoConexaoHost.WINRM);
    UUID winId = UUID.randomUUID();
    setId(windows, winId);
    when(hostRepository.findById(winId)).thenReturn(Optional.of(windows));

    InstalacaoClienteRequest request = new InstalacaoClienteRequest(
        "x", "x", clienteId, winId, produtoId,
        TipoImplantacao.LINUX_MANUAL, StatusInstalacao.INEXISTENTE, AmbientePadrao.PROD,
        null, null, null, null, null, null, null);

    assertThatThrownBy(() -> service.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Linux");
  }

  @Test
  void criar_windowsManualExigeHostWindows() {
    assertThatThrownBy(() -> service.criar(req(
        "x", "x", TipoImplantacao.WINDOWS_MANUAL, StatusInstalacao.INEXISTENTE,
        null, null, "C:\\Nexus\\RPA")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Windows");
  }

  @Test
  void criar_dockerExigeHostComDocker() {
    hostLinuxDocker.setDockerDisponivel(false);

    assertThatThrownBy(() -> service.criar(req(
        "x", "x", TipoImplantacao.DOCKER_PULL, StatusInstalacao.INEXISTENTE,
        "nexus/rpa:1", null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Docker");
  }

  @Test
  void criar_linuxManualAtivaExigeDiretorio() throws Exception {
    Host linux = new Host("SRV-LIN-02", "Lin", "lin-02",
        SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    UUID linuxId = UUID.randomUUID();
    setId(linux, linuxId);
    when(hostRepository.findById(linuxId)).thenReturn(Optional.of(linux));

    InstalacaoClienteRequest request = new InstalacaoClienteRequest(
        "x", "x", clienteId, linuxId, produtoId,
        TipoImplantacao.LINUX_MANUAL, StatusInstalacao.ATIVA, AmbientePadrao.PROD,
        null, null, null, null, null, null, null);

    assertThatThrownBy(() -> service.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("diretório");
  }

  @Test
  void criar_falhaCodigoDuplicado() {
    when(repository.existsByCodigoIgnoreCase("ACME-RPA-01")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(req(
        "acme-rpa-01", "RPA", TipoImplantacao.DOCKER_PULL, StatusInstalacao.INEXISTENTE,
        null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("código");
  }

  @Test
  void criar_falhaAlvoDuplicado() {
    when(repository.existsByCliente_IdAndProduto_IdAndHost_IdAndAmbiente(
        clienteId, produtoId, hostId, AmbientePadrao.PROD)).thenReturn(true);

    assertThatThrownBy(() -> service.criar(req(
        "outro", "RPA", TipoImplantacao.DOCKER_PULL, StatusInstalacao.INEXISTENTE,
        "nexus/rpa:1", null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Já existe instalação");
  }

  private InstalacaoClienteRequest req(String codigo, String nome, TipoImplantacao tipo,
      StatusInstalacao status, String imagem, String tar, String dir) {
    return new InstalacaoClienteRequest(
        codigo, nome, clienteId, hostId, produtoId, tipo, status, AmbientePadrao.PROD,
        imagem, tar, dir, null, null, null, null);
  }

  @Test
  void registrarHealth_gravaVersaoErroETimestamp() throws Exception {
    UUID id = UUID.randomUUID();
    InstalacaoCliente inst = new InstalacaoCliente(
        "ACME-RPA-01", "RPA", cliente, hostLinuxDocker, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    setId(inst, id);
    when(repository.findById(id)).thenReturn(Optional.of(inst));

    InstalacaoCliente out = service.registrarHealth(id, new RegistrarHealthInstalacaoRequest(
        HealthInstalacao.DEGRADADO, "1.4.0", "timeout no /health"));

    assertThat(out.getHealth()).isEqualTo(HealthInstalacao.DEGRADADO);
    assertThat(out.getVersaoAtual()).isEqualTo("1.4.0");
    assertThat(out.getUltimoErro()).isEqualTo("timeout no /health");
    assertThat(out.getUltimaVerificacao()).isNotNull();
  }

  @Test
  void excluir_bloqueiaQuandoVinculadaAEntrega() throws Exception {
    UUID id = UUID.randomUUID();
    InstalacaoCliente inst = new InstalacaoCliente(
        "ACME-RPA-01", "RPA", cliente, hostLinuxDocker, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    setId(inst, id);
    when(repository.findById(id)).thenReturn(Optional.of(inst));
    when(entregaInstalacaoRepository.existsByInstalacao_Id(id)).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("vinculada");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
