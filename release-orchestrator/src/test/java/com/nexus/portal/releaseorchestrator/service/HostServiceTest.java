package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.HostRequest;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorHostRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HostServiceTest {

  @Mock OrchestratorHostRepository repository;
  @Mock OrchestratorInstalacaoClienteRepository instalacaoRepository;
  @InjectMocks HostService service;

  @Test
  void criar_normalizaCodigoUppercaseEDefineDefaultsLinux() {
    HostRequest req = req("srv-lin-01", "Srv Linux 01", "srv-linux-01", null,
        SistemaOperacionalHost.LINUX, null, null, null);
    when(repository.existsByCodigoIgnoreCase("SRV-LIN-01")).thenReturn(false);
    when(repository.existsByHostnameIgnoreCase("srv-linux-01")).thenReturn(false);
    when(repository.save(any(Host.class))).thenAnswer(inv -> inv.getArgument(0));

    Host host = service.criar(req);

    assertThat(host.getCodigo()).isEqualTo("SRV-LIN-01");
    assertThat(host.getTipoConexao()).isEqualTo(TipoConexaoHost.SSH);
    assertThat(host.getPortaConexao()).isEqualTo(22);
    assertThat(host.isDockerDisponivel()).isFalse();
    assertThat(host.isAtivo()).isTrue();
  }

  @Test
  void criar_windowsAssumeWinrmEPorta5985() {
    HostRequest req = req("srv-win-01", "Srv Windows", "srv-windows-01", null,
        SistemaOperacionalHost.WINDOWS, null, null, null);
    when(repository.save(any(Host.class))).thenAnswer(inv -> inv.getArgument(0));

    Host host = service.criar(req);

    assertThat(host.getTipoConexao()).isEqualTo(TipoConexaoHost.WINRM);
    assertThat(host.getPortaConexao()).isEqualTo(5985);
  }

  @Test
  void criar_tipoDockerMarcaDockerDisponivelEPorta2376() {
    HostRequest req = req("dock-01", "Docker host", "dock-01.local", null,
        SistemaOperacionalHost.LINUX, false, TipoConexaoHost.DOCKER, null);
    when(repository.save(any(Host.class))).thenAnswer(inv -> inv.getArgument(0));

    Host host = service.criar(req);

    assertThat(host.getTipoConexao()).isEqualTo(TipoConexaoHost.DOCKER);
    assertThat(host.isDockerDisponivel()).isTrue();
    assertThat(host.getPortaConexao()).isEqualTo(2376);
  }

  @Test
  void criar_falhaCodigoDuplicado() {
    when(repository.existsByCodigoIgnoreCase("SRV-01")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(req("srv-01", "Srv", "srv-01", null,
        SistemaOperacionalHost.LINUX, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("código");
  }

  @Test
  void criar_falhaHostnameDuplicado() {
    when(repository.existsByCodigoIgnoreCase(any())).thenReturn(false);
    when(repository.existsByHostnameIgnoreCase("srv-01")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(req("outro", "Outro", "srv-01", null,
        SistemaOperacionalHost.LINUX, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("hostname");
  }

  @Test
  void criar_rejeitaIpInvalido() {
    assertThatThrownBy(() -> service.criar(req("srv-01", "Srv", "srv-01", "999.1.1.1",
        SistemaOperacionalHost.LINUX, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("IP");
  }

  @Test
  void criar_aceitaIpv4Valido() {
    HostRequest req = req("srv-01", "Srv", "srv-01", "10.0.0.12",
        SistemaOperacionalHost.LINUX, true, TipoConexaoHost.SSH, 22);
    when(repository.save(any(Host.class))).thenAnswer(inv -> inv.getArgument(0));

    Host host = service.criar(req);

    assertThat(host.getEnderecoIp()).isEqualTo("10.0.0.12");
    assertThat(host.isDockerDisponivel()).isTrue();
  }

  @Test
  void atualizar_persisteCamposEditaveis() {
    UUID id = UUID.randomUUID();
    Host existente = new Host("SRV-01", "Antigo", "old-host", SistemaOperacionalHost.LINUX,
        TipoConexaoHost.SSH);
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    Host atualizado = service.atualizar(id, req("srv-01", "Novo nome", "new-host", "192.168.1.10",
        SistemaOperacionalHost.LINUX, true, TipoConexaoHost.SSH, 2222));

    assertThat(atualizado.getNome()).isEqualTo("Novo nome");
    assertThat(atualizado.getHostname()).isEqualTo("new-host");
    assertThat(atualizado.getPortaConexao()).isEqualTo(2222);
    assertThat(atualizado.isDockerDisponivel()).isTrue();
  }

  @Test
  void alterarStatus_atualiza() {
    UUID id = UUID.randomUUID();
    Host host = new Host("SRV-01", "Srv", "srv-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    when(repository.findById(id)).thenReturn(Optional.of(host));

    Host inativo = service.alterarStatus(id, false);
    assertThat(inativo.isAtivo()).isFalse();
  }

  @Test
  void buscar_lancaNotFoundQuandoAusente() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void excluir_chamaDelete() {
    UUID id = UUID.randomUUID();
    Host host = new Host("SRV-01", "Srv", "srv-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    when(repository.findById(id)).thenReturn(Optional.of(host));
    when(instalacaoRepository.existsByHost_Id(id)).thenReturn(false);

    service.excluir(id);
    verify(repository).delete(host);
  }

  @Test
  void excluir_falhaSeHaInstalacaoVinculada() {
    UUID id = UUID.randomUUID();
    Host host = new Host("SRV-01", "Srv", "srv-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    when(repository.findById(id)).thenReturn(Optional.of(host));
    when(instalacaoRepository.existsByHost_Id(id)).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("instalações");
  }

  private HostRequest req(String codigo, String nome, String hostname, String ip,
      SistemaOperacionalHost so, Boolean docker, TipoConexaoHost tipo, Integer porta) {
    return new HostRequest(codigo, nome, hostname, ip, so, docker, tipo, porta, null, null, null, null);
  }
}
