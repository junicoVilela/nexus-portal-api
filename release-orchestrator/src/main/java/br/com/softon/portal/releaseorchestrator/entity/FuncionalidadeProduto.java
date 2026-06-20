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
 * Funcionalidade dentro de um domínio do catálogo do produto. Distinta da
 * {@code Funcionalidade} do RBAC em DocFlow (que representa itens de
 * segurança como USUARIO, CLIENTE, etc.).
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorFuncionalidadeProduto")
@Table(name = "tb_funcionalidade_produto")
public class FuncionalidadeProduto extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dominio_produto_id", nullable = false)
  private DominioProduto dominio;

  @Column(nullable = false, length = 200)
  private String nome;

  /** Slug interno. Único por domínio. */
  @Column(nullable = false, length = 80)
  private String codigo;

  /** `CD_FUNCIONALIDADE` no DB do cliente legado. */
  @Column(name = "codigo_legado", length = 80)
  private String codigoLegado;

  /** `CD_OPERACAO` no DB do cliente legado. */
  @Column(name = "codigo_operacao", length = 80)
  private String codigoOperacao;

  @Column(length = 1000)
  private String descricao;

  /** Funcionalidade crítica = não pode ser desabilitada por cliente. */
  @Column(nullable = false)
  private boolean critica;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  public FuncionalidadeProduto(DominioProduto dominio, String nome, String codigo,
      String codigoLegado, String codigoOperacao, String descricao,
      boolean critica, int ordem) {
    this.dominio = dominio;
    this.nome = nome.trim();
    this.codigo = codigo;
    this.codigoLegado = codigoLegado;
    this.codigoOperacao = codigoOperacao;
    this.descricao = descricao;
    this.critica = critica;
    this.ordem = ordem;
    this.ativo = true;
  }

  public void atualizar(String nome, String codigoLegado, String codigoOperacao,
      String descricao, boolean critica, int ordem) {
    this.nome = nome.trim();
    this.codigoLegado = codigoLegado;
    this.codigoOperacao = codigoOperacao;
    this.descricao = descricao;
    this.critica = critica;
    this.ordem = ordem;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }
}
