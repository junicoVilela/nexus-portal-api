package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.ReservaPortaRequest;
import com.nexus.portal.releaseorchestrator.dto.response.PortasSugeridasResponse;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.PapelPorta;
import com.nexus.portal.releaseorchestrator.entity.ProtocoloPorta;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoPorta;
import com.nexus.portal.releaseorchestrator.integration.ssh.HostPortProbe;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorHostRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorReservaPortaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class ReservaPortaServiceTest {

  @Mock OrchestratorReservaPortaRepository repository;
  @Mock OrchestratorHostRepository hostRepository;
  @Mock HostPortProbe hostPortProbe;
  @InjectMocks ReservaPortaService service;
  Host host;
  UUID hostId;

  @BeforeEach
  void setUp() {
    hostId = UUID.randomUUID();
    host = new Host("SRV", "Srv", "cli-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));
    when(hostPortProbe.emUso(any())).thenReturn(Set.of());
    when(hostPortProbe.emUsoLocal()).thenReturn(Set.of());
  }

  @Test
  void sugerir_devolveTresBackendAPartirDe8081ETresFrontendAPartirDe4000() {
    when(repository.findByHost_IdAndStatusIn(hostId, ReservaPorta.STATUS_OCUPADOS)).thenReturn(List.of());

    PortasSugeridasResponse r = service.sugerir(hostId, null);

    assertThat(r.backend()).containsExactly(8081, 8082, 8083);
    assertThat(r.frontend()).containsExactly(4000, 4001, 4002);
    assertThat(r.verificouHost()).isTrue();
  }

  @Test
  void sugerir_pulaPortasOcupadasNoInventarioENoHost() {
    ReservaPorta ocupada = new ReservaPorta(null, null, TipoPorta.HTTP, PapelPorta.BACKEND, 8081,
        ProtocoloPorta.TCP, StatusReservaPorta.RESERVADA);
    when(repository.findByHost_IdAndStatusIn(hostId, ReservaPorta.STATUS_OCUPADOS))
        .thenReturn(List.of(ocupada));
    when(hostPortProbe.emUso(any())).thenReturn(Set.of(8082, 4000));

    PortasSugeridasResponse r = service.sugerir(hostId, null);

    assertThat(r.backend()).containsExactly(8083, 8084, 8085);
    assertThat(r.frontend()).containsExactly(4001, 4002, 4003);
    assertThat(r.emUsoNoHost()).contains(8082, 4000);
  }

  @Test
  void sugerir_linuxUsa8010E4209SeLivres() {
    when(repository.findByHost_IdAndStatusIn(hostId, ReservaPorta.STATUS_OCUPADOS)).thenReturn(List.of());

    PortasSugeridasResponse r = service.sugerir(hostId, null, 8010, 4209, 1);

    assertThat(r.backend()).containsExactly(8010);
    assertThat(r.frontend()).containsExactly(4209);
  }

  @Test
  void validar_rejeitaFrontendAbaixoDe4000() {
    ReservaPortaRequest req = new ReservaPortaRequest(
        TipoPorta.HTTP, PapelPorta.FRONTEND, 3999, ProtocoloPorta.TCP, StatusReservaPorta.RESERVADA);

    assertThatThrownBy(() -> service.validarReservas(UUID.randomUUID(), null, List.of(req)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("4000");
  }

  @Test
  void validar_rejeitaDuplicataNoPedido() {
    ReservaPortaRequest a = new ReservaPortaRequest(
        TipoPorta.HTTP, PapelPorta.BACKEND, 8081, ProtocoloPorta.TCP, StatusReservaPorta.RESERVADA);
    ReservaPortaRequest b = new ReservaPortaRequest(
        TipoPorta.HTTPS, PapelPorta.BACKEND, 8081, ProtocoloPorta.TCP, StatusReservaPorta.RESERVADA);

    assertThatThrownBy(() -> service.validarReservas(UUID.randomUUID(), null, List.of(a, b)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("duplicada");
  }

  @Test
  void validar_rejeitaConflitoNoHost() {
    UUID hostId = UUID.randomUUID();
    when(repository.existsByHost_IdAndPortaAndProtocoloAndStatusIn(
        hostId, 8081, ProtocoloPorta.TCP, ReservaPorta.STATUS_OCUPADOS)).thenReturn(true);
    ReservaPortaRequest req = new ReservaPortaRequest(
        TipoPorta.HTTP, PapelPorta.BACKEND, 8081, ProtocoloPorta.TCP, StatusReservaPorta.RESERVADA);

    assertThatThrownBy(() -> service.validarReservas(hostId, null, List.of(req)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já está reservada");
  }

  @Test
  void proximasLivres_respeitaMinimoEOcupadas() {
    Set<Integer> ocupadas = new HashSet<>(Set.of(4000, 4001));
    assertThat(ReservaPortaService.proximasLivres(4000, 3, ocupadas, 4000))
        .containsExactly(4002, 4003, 4004);
  }
}
