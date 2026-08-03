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
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrega planejada (não executada ainda). Liga cliente + produto + release
 * com data prevista e ambiente. Quando o operador cria a Entrega real
 * (via assistente F1.8), a ProximaEntrega vai para {@code CONVERTIDA} e
 * {@code entregaConvertidaId} aponta para a Entrega gerada.
 *
 * Spec: docs/release-orchestrator/16-proximas-entregas-agenda.md
 *       docs/release-orchestrator/17-proximas-entregas-cadastro.md
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorProximaEntrega")
@Table(name = "tb_proxima_entrega")
public class ProximaEntrega extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  /** Release alvo (opcional na planejamento inicial). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "release_id")
  private Release release;

  @Column(name = "data_prevista", nullable = false)
  private LocalDate dataPrevista;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AmbientePadrao ambiente;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PrioridadeEntrega prioridade;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusProximaEntrega status;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  /** Preenchido quando a próxima entrega é convertida em Entrega (F1.8). */
  @Column(name = "entrega_convertida_id")
  private UUID entregaConvertidaId;

  public ProximaEntrega(Cliente cliente, ProdutoRh produto, Release release,
      LocalDate dataPrevista, AmbientePadrao ambiente, PrioridadeEntrega prioridade,
      UUID responsavelId, String observacoes) {
    this.cliente = cliente;
    this.produto = produto;
    this.release = release;
    this.dataPrevista = dataPrevista;
    this.ambiente = ambiente;
    this.prioridade = prioridade;
    this.status = StatusProximaEntrega.PLANEJADA;
    this.responsavelId = responsavelId;
    this.observacoes = observacoes;
  }

  public void atualizar(Release release, LocalDate dataPrevista, AmbientePadrao ambiente,
      PrioridadeEntrega prioridade, UUID responsavelId, String observacoes) {
    this.release = release;
    this.dataPrevista = dataPrevista;
    this.ambiente = ambiente;
    this.prioridade = prioridade;
    this.responsavelId = responsavelId;
    this.observacoes = observacoes;
  }

  public void alterarStatus(StatusProximaEntrega novo) {
    this.status = novo;
  }

  public void marcarConvertida(UUID entregaId) {
    this.entregaConvertidaId = entregaId;
    this.status = StatusProximaEntrega.CONVERTIDA;
  }
}
