package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
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

/**
 * Linha da seleção de módulos de uma {@link Entrega} (F1.9).
 *
 * Origem dos campos versão:
 * <ul>
 *   <li>{@code versaoFrom} = {@link ClienteProdutoModulo#getVersaoAtual()}
 *       no momento da inicialização.</li>
 *   <li>{@code versaoTo} = {@link ReleaseModuloVersao#getVersao()} da
 *       release da entrega.</li>
 * </ul>
 *
 * Spec: docs/release-orchestrator/19-selecao-modulos.md
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorEntregaModulo")
@Table(name = "tb_entrega_modulo")
public class EntregaModulo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entrega_id", nullable = false)
  private Entrega entrega;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modulo_produto_id", nullable = false)
  private ModuloProduto moduloProduto;

  @Column(name = "versao_from", length = 80)
  private String versaoFrom;

  @Column(name = "versao_to", length = 80)
  private String versaoTo;

  @Column(nullable = false)
  private boolean selecionado;

  /** Marcado true quando o cliente não contrata esse módulo mas ele veio na release. */
  @Column(name = "fora_contrato", nullable = false)
  private boolean foraContrato;

  @Column(nullable = false)
  private int ordem;

  public EntregaModulo(Entrega entrega, ModuloProduto moduloProduto, String versaoFrom,
      String versaoTo, boolean selecionado, boolean foraContrato, int ordem) {
    this.entrega = entrega;
    this.moduloProduto = moduloProduto;
    this.versaoFrom = versaoFrom;
    this.versaoTo = versaoTo;
    this.selecionado = selecionado;
    this.foraContrato = foraContrato;
    this.ordem = ordem;
  }

  public void alterarSelecao(boolean selecionado) {
    this.selecionado = selecionado;
  }
}
