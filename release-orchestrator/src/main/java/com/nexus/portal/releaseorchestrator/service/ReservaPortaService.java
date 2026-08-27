package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.ReservaPortaRequest;
import com.nexus.portal.releaseorchestrator.dto.response.PortasSugeridasResponse;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.PapelPorta;
import com.nexus.portal.releaseorchestrator.entity.ProtocoloPorta;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.integration.ssh.HostPortProbe;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorHostRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorReservaPortaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventário de portas por host (RF-005). Conflito: mesmo host + porta + protocolo ocupados.
 * Sugestão considera reservas e, quando possível, portas em LISTEN no host.
 */
@Service("orchestratorReservaPortaService")
@RequiredArgsConstructor
public class ReservaPortaService {

  public static final int BACKEND_INICIO = 8081;
  public static final int FRONTEND_INICIO = 4000;
  public static final int LINUX_BACKEND_INICIO = 8010;
  public static final int LINUX_FRONTEND_INICIO = 4209;
  public static final int QUANTIDADE = 3;

  private final OrchestratorReservaPortaRepository repository;
  private final OrchestratorHostRepository hostRepository;
  private final HostPortProbe hostPortProbe;

  @Transactional(readOnly = true)
  public PortasSugeridasResponse sugerir(UUID hostId, UUID instalacaoIgnorada) {
    return sugerir(hostId, instalacaoIgnorada, BACKEND_INICIO, FRONTEND_INICIO, QUANTIDADE);
  }

  @Transactional(readOnly = true)
  public PortasSugeridasResponse sugerir(
      UUID hostId, UUID instalacaoIgnorada, int backendInicio, int frontendInicio, int quantidade) {
    int qtd = quantidade < 1 ? 1 : Math.min(quantidade, 10);
    int be = backendInicio < 1 ? BACKEND_INICIO : backendInicio;
    int fe = frontendInicio < 1 ? FRONTEND_INICIO : frontendInicio;
    Host host = hostId == null ? null : hostRepository.findById(hostId)
        .orElseThrow(() -> new NotFoundException("Host não encontrado."));
    Set<Integer> ocupadas = hostId == null
        ? new HashSet<>()
        : portasTcpOcupadas(hostId, instalacaoIgnorada);
    Set<Integer> emUsoHost = host == null ? hostPortProbe.emUsoLocal() : hostPortProbe.emUso(host);
    ocupadas.addAll(emUsoHost);
    List<Integer> backend = proximasLivres(be, qtd, ocupadas, 1);
    ocupadas.addAll(backend);
    List<Integer> frontend = proximasLivres(fe, qtd, ocupadas, FRONTEND_INICIO);
    return new PortasSugeridasResponse(backend, frontend, List.copyOf(emUsoHost), true);
  }

  void validarReservas(UUID hostId, UUID instalacaoAtual, List<ReservaPortaRequest> portas) {
    if (portas == null || portas.isEmpty()) {
      return;
    }
    Set<String> noPedido = new HashSet<>();
    for (ReservaPortaRequest req : portas) {
      ProtocoloPorta protocolo = req.protocolo() == null ? ProtocoloPorta.TCP : req.protocolo();
      StatusReservaPorta status = req.status() == null ? StatusReservaPorta.RESERVADA : req.status();
      if (req.papel() == PapelPorta.FRONTEND
          && req.porta() < FRONTEND_INICIO) {
        throw new BusinessException("Porta de frontend deve ser a partir de " + FRONTEND_INICIO + ".");
      }
      String chave = protocolo.name() + ":" + req.porta();
      if (!noPedido.add(chave)) {
        throw new BusinessException("A porta " + req.porta() + "/" + protocolo + " está duplicada nesta instalação.");
      }
      if (!ReservaPorta.STATUS_OCUPADOS.contains(status)) {
        continue;
      }
      boolean conflito = instalacaoAtual == null
          ? repository.existsByHost_IdAndPortaAndProtocoloAndStatusIn(
              hostId, req.porta(), protocolo, ReservaPorta.STATUS_OCUPADOS)
          : repository.existsByHost_IdAndPortaAndProtocoloAndStatusInAndInstalacao_IdNot(
              hostId, req.porta(), protocolo, ReservaPorta.STATUS_OCUPADOS, instalacaoAtual);
      if (conflito) {
        throw new BusinessException(
            "A porta " + req.porta() + "/" + protocolo + " já está reservada neste host.");
      }
    }
  }

  static List<Integer> proximasLivres(int inicio, int quantidade, Set<Integer> ocupadas, int minimo) {
    List<Integer> livres = new ArrayList<>();
    int candidata = Math.max(inicio, minimo);
    while (livres.size() < quantidade && candidata <= 65535) {
      if (!ocupadas.contains(candidata)) {
        livres.add(candidata);
      }
      candidata++;
    }
    if (livres.size() < quantidade) {
      throw new BusinessException("Não há portas livres suficientes neste host.");
    }
    return livres;
  }

  private Set<Integer> portasTcpOcupadas(UUID hostId, UUID instalacaoIgnorada) {
    Set<Integer> ocupadas = new HashSet<>();
    for (ReservaPorta reserva : repository.findByHost_IdAndStatusIn(hostId, ReservaPorta.STATUS_OCUPADOS)) {
      if (instalacaoIgnorada != null && instalacaoIgnorada.equals(reserva.getInstalacao().getId())) {
        continue;
      }
      if (reserva.getProtocolo() == ProtocoloPorta.TCP) {
        ocupadas.add(reserva.getPorta());
      }
    }
    return ocupadas;
  }
}
