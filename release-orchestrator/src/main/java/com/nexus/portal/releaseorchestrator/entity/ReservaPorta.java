package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reserva de porta no host para uma instalação (RF-005). */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorReservaPorta")
@Table(name = "tb_reserva_porta")
public class ReservaPorta extends AuditableEntity {

  public static final Set<StatusReservaPorta> STATUS_OCUPADOS =
      EnumSet.of(StatusReservaPorta.RESERVADA, StatusReservaPorta.EM_USO, StatusReservaPorta.BLOQUEADA);

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "host_id", nullable = false)
  private Host host;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instalacao_id", nullable = false)
  private InstalacaoCliente instalacao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoPorta tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PapelPorta papel;

  @Column(nullable = false)
  private int porta;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private ProtocoloPorta protocolo = ProtocoloPorta.TCP;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusReservaPorta status = StatusReservaPorta.RESERVADA;

  public ReservaPorta(Host host, InstalacaoCliente instalacao, TipoPorta tipo, PapelPorta papel, int porta,
      ProtocoloPorta protocolo, StatusReservaPorta status) {
    this.host = host;
    this.instalacao = instalacao;
    this.tipo = tipo;
    this.papel = papel;
    this.porta = porta;
    this.protocolo = protocolo;
    this.status = status;
  }

  public boolean ocupaHost() {
    return STATUS_OCUPADOS.contains(status);
  }

  public void marcarEmUso() {
    if (status == StatusReservaPorta.RESERVADA || status == StatusReservaPorta.EM_USO) {
      this.status = StatusReservaPorta.EM_USO;
    }
  }
}
