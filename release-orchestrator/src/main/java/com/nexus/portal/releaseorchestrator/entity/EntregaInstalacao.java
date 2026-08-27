package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vínculo entrega → instalação alvo (RF-008). Nunca aponta para host. */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorEntregaInstalacao")
@Table(name = "tb_entrega_instalacao")
public class EntregaInstalacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entrega_id", nullable = false)
  private Entrega entrega;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instalacao_id", nullable = false)
  private InstalacaoCliente instalacao;

  public EntregaInstalacao(Entrega entrega, InstalacaoCliente instalacao) {
    this.entrega = entrega;
    this.instalacao = instalacao;
  }
}
