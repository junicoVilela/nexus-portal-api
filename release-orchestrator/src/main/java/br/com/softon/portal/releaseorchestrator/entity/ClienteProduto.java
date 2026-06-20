package br.com.softon.portal.releaseorchestrator.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrato de produto para um cliente. Define qual ambiente é alvo das
 * entregas deste cliente para este produto.
 *
 * Spec: docs/release-orchestrator/06-cliente-produtos-contratados.md
 *       docs/release-orchestrator/32-modelo-dados-sugerido.md (ClienteProduto)
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorClienteProduto")
@Table(name = "tb_cliente_produto")
public class ClienteProduto extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AmbientePadrao ambiente;

  @Column(nullable = false)
  private boolean ativo = true;

  public ClienteProduto(Cliente cliente, ProdutoRh produto, AmbientePadrao ambiente) {
    this.cliente = cliente;
    this.produto = produto;
    this.ambiente = ambiente;
    this.ativo = true;
  }

  public void atualizar(AmbientePadrao ambiente, boolean ativo) {
    this.ambiente = ambiente;
    this.ativo = ativo;
  }
}
