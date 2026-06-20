package br.com.softon.portal.releaseorchestrator.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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
 * Domínio funcional do produto (Camada A do catálogo). Distinto do
 * {@code Dominio} do RBAC em DocFlow (que representa segurança como
 * SEGURANCA, DOC_FLOW, etc.). Sufixo "Produto" intencional pra evitar
 * colisão de nome — ver spec 32 §4 e 33 §Autorização.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorDominioProduto")
@Table(name = "tb_dominio_produto")
public class DominioProduto extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @Column(nullable = false, length = 150)
  private String nome;

  /** Slug interno (snake_case ou kebab-case). Único por produto. */
  @Column(nullable = false, length = 80)
  private String codigo;

  /** Código original do sistema legado (`CD_DOMINIO_FUNCIONAL` no DB do cliente). */
  @Column(name = "codigo_legado", length = 80)
  private String codigoLegado;

  @Column(length = 1000)
  private String descricao;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  public DominioProduto(ProdutoRh produto, String nome, String codigo, String codigoLegado,
      String descricao, int ordem) {
    this.produto = produto;
    this.nome = nome.trim();
    this.codigo = codigo;
    this.codigoLegado = codigoLegado;
    this.descricao = descricao;
    this.ordem = ordem;
    this.ativo = true;
  }

  public void atualizar(String nome, String codigoLegado, String descricao, int ordem) {
    this.nome = nome.trim();
    this.codigoLegado = codigoLegado;
    this.descricao = descricao;
    this.ordem = ordem;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }
}
